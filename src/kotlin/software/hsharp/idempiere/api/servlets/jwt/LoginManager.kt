package software.hsharp.idempiere.api.servlets.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import org.compiere.Adempiere
import org.compiere.model.MUser
import org.compiere.util.Login
import org.idempiere.common.exceptions.AdempiereException
import org.idempiere.common.util.*
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import software.hsharp.api.helpers.jwt.*
import software.hsharp.api.icommon.IDatabase
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level

@Component
class LoginServiceRegisterHolder {
	companion object {
	    var LoginServiceRegister: ILoginServiceRegister? = null
		var loginManager : LoginManager = LoginManager()
	}

	@Reference
	fun setDatabase(loginServiceRegister: ILoginServiceRegister) {
		LoginServiceRegister = loginServiceRegister
		loginServiceRegister.registerLoginService( loginManager )
	}

}

class LoginManager : ILoginService {
	override val uniqueKey: String
		get() = LOGIN_MANAGER_UNIQUE_KEY

	override fun login(login: ILogin): ILoginResponse {
		return doLogin( login )
	}

	val LOGIN_MANAGER_UNIQUE_KEY  = "software.hsharp.idempiere.api.servlets.jwt.LoginManager"
	val AUTH_HEADER_KEY = "Authorization"
	val AUTH_HEADER_VALUE_PREFIX = "Bearer " // with trailing space to separate token

	private val log = CLogger.getCLogger(LoginManager::class.java)

	val dateFormatOnlyForCtx = "yyyy-MM-dd"

	val C_BPARTNER_ID = "#C_BPartner_Id"


	/**
	 * Check Login information and set context.
	 * @returns    true if login info are OK
	 * @param ctx context
	 * @param AD_User_ID user
	 * @param AD_Role_ID role
	 * @param AD_Client_ID client
	 * @param AD_Org_ID org
	 * @param M_Warehouse_ID warehouse
	 */
	private fun checkLogin(ctx: Properties, AD_User_ID: Int, AD_Role_ID: Int, AD_Client_ID: Int, AD_Org_ID: Int, M_Warehouse_ID: Int): KeyNamePair? {
		//  Get Login Info
		var loginInfo: String? = null
		var c_bpartner_id = -1
		//  Verify existence of User/Client/Org/Role and User's access to Client & Org
		val sql = ("SELECT u.Name || '@' || c.Name || '.' || o.Name AS Text, u.c_bpartner_id "
				+ "FROM AD_User u, AD_Client c, AD_Org o, AD_User_Roles ur "
				+ "WHERE u.AD_User_ID=?"    //  #1

				+ " AND c.AD_Client_ID=?"   //  #2

				+ " AND o.AD_Org_ID=?"      //  #3

				+ " AND ur.AD_Role_ID=?"    //  #4

				+ " AND ur.AD_User_ID=u.AD_User_ID"
				+ " AND (o.AD_Client_ID = 0 OR o.AD_Client_ID=c.AD_Client_ID)"
				+ " AND c.AD_Client_ID IN (SELECT AD_Client_ID FROM AD_Role_OrgAccess ca WHERE ca.AD_Role_ID=ur.AD_Role_ID)"
				+ " AND o.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_Role_OrgAccess ca WHERE ca.AD_Role_ID=ur.AD_Role_ID)")
		var pstmt: PreparedStatement? = null
		var rs: ResultSet? = null
		try {
			pstmt = DB.prepareStatement(sql, null)
			pstmt!!.setInt(1, AD_User_ID)
			pstmt.setInt(2, AD_Client_ID)
			pstmt.setInt(3, AD_Org_ID)
			pstmt.setInt(4, AD_Role_ID)
			rs = pstmt.executeQuery()
			if (rs!!.next()) {
				loginInfo = rs.getString(1)
				c_bpartner_id = rs.getInt(2)
			}
		} catch (e: SQLException) {
			e.printStackTrace()
		} finally {
			DB.close(rs, pstmt)
			rs = null
			pstmt = null
		}

		//  not verified
		if (loginInfo == null)
			return null

		//  Set Preferences
		val org = KeyNamePair(AD_Org_ID, AD_Org_ID.toString())
		var wh: KeyNamePair? = null
		if (M_Warehouse_ID > 0)
			wh = KeyNamePair(M_Warehouse_ID, M_Warehouse_ID.toString())
		//
		val date: Timestamp? = null
		val printer: String? = null
		val login = Login(ctx)
		login.loadPreferences(org, wh, date, printer)
		//
		return KeyNamePair(c_bpartner_id, loginInfo)
	}   //  checkLogin


	/**
	 *
	 * @param AD_User_ID
	 * @param AD_Role_ID
	 * @param AD_Client_ID
	 * @param AD_Org_ID
	 * @param M_Warehouse_ID
	 * @param Lang
	 * @return true if login is successful
	 */
	fun login(ctx: Properties, AD_User_ID: Int, AD_Role_ID: Int, AD_Client_ID: Int, AD_Org_ID: Int, M_Warehouse_ID: Int, Lang: String): Boolean {
		val loginInfoFull = checkLogin(ctx, AD_User_ID, AD_Role_ID, AD_Client_ID, AD_Org_ID, M_Warehouse_ID)
				?: return false
		val loginInfo = loginInfoFull!!.getName()
		val c_bpartner_id = loginInfoFull!!.getKey()

		Env.setContext(ctx, "#AD_Language", Lang)
		val m_language = Language.getLanguage(Lang)
		Env.verifyLanguage(ctx, m_language)

		//  Set Date
		val ts = Timestamp(System.currentTimeMillis())

		val dateFormat4Timestamp = SimpleDateFormat(dateFormatOnlyForCtx)
		Env.setContext(ctx, "#Date", dateFormat4Timestamp.format(ts) + " 00:00:00")    //  JDBC format
		if (log.isLoggable(Level.INFO)) log.info(" #Date = " + Env.getContextAsDate(ctx, "#Date"))

		Env.setContext(ctx, "#M_Warehouse_ID", M_Warehouse_ID)
		Env.setContext(ctx, Env.LANGUAGE, m_language.aD_Language)
		Env.setContext(ctx, C_BPARTNER_ID, c_bpartner_id)

		return true
	}

    fun doLogin( login : ILogin ) : UserLoginModelResponse {
        Adempiere.getI().startup(false)

        val mapper = ObjectMapper()
        val ctx = Env.getCtx()
        val loginUtil = Login(ctx)

        // HACK - this is needed before calling the list of clients, because the user will be logged in
        // HACK - and the information about the login success or failure need to be saved to the DB
        ctx.setProperty(Env.AD_CLIENT_ID, "0" )
        Env.setContext(ctx, Env.AD_CLIENT_ID, "0" )

        val clients = loginUtil.getClients(login.loginName, login.password)
        if (clients == null) {
            throw AdempiereException("Error login - User invalid")
        }

        val selectedClientIndex = clients.indexOfFirst { clients.count() == 1 }

        val roles =
                if (selectedClientIndex == -1 ) {
                    null
                } else {
                    val clientId = clients[selectedClientIndex].id
                    ctx.setProperty(Env.AD_CLIENT_ID, clientId )
                    Env.setContext(ctx, Env.AD_CLIENT_ID, clientId )
                    loginUtil.getRoles(login.loginName, clients[selectedClientIndex] );
                }

        val user = MUser.get (ctx, login.loginName);
        if (user != null) {
            Env.setContext(ctx, Env.AD_USER_ID, user.getAD_User_ID() )
            Env.setContext(ctx, "#AD_User_Name", user.getName() )
            Env.setContext(ctx, "#SalesRep_ID", user.getAD_User_ID() )
        }

        val selectedRoleIndex =
                if (roles==null) { -1 }
                else { roles.indexOfFirst { roles.count() == 1 } }

        // orgs
        val orgs =
                if (selectedRoleIndex == -1 ) {
                    null
                } else {
                    loginUtil.getOrgs( roles!![selectedRoleIndex] );
                }

        val selectedOrgIndex =
                if (orgs==null) { -1 }
                else { orgs.indexOfFirst { orgs.count() == 1 } }

        // warehouses
        val warehouses =
                if (selectedOrgIndex == -1 ) {
                    null
                } else {
                    loginUtil.getWarehouses( orgs!![selectedOrgIndex] );
                }

        val selectedWarehouseIndex =
                if (warehouses==null) { -1 }
                else { warehouses.indexOfFirst { warehouses.count() == 1  } }

        val AD_User_ID = Env.getAD_User_ID(ctx)

        val logged =
                ( selectedWarehouseIndex != -1 ) &&
                        ( selectedOrgIndex != -1 ) &&
                        ( selectedRoleIndex != -1 ) &&
                        ( selectedClientIndex != -1 ) &&
                        login(
                                ctx,
                                AD_User_ID,
                                roles!![selectedRoleIndex].key,
                                clients[selectedClientIndex].key,
                                orgs!![selectedOrgIndex].key,
                                warehouses!![selectedWarehouseIndex].key,
                                "en_US" )

        val result = UserLoginModelResponse( logged, clients, roles, orgs, warehouses, null )

        if (result.logged) {
			//val realLogin : UserLoginModel = UserLoginModel( login.loginName, login.password )
            val token = JwtManager.createToken( AD_User_ID.toString(), "",
                    /*mapper.writeValueAsString(realLogin)*/ "{ \"loginName\":\"" + login.loginName + "\", \"password\":\"" + login.password + "\"}" )
            return result.copy(token=token)
        } else {
            return result
        }
    }

	fun doLogin( login : UserLoginModel ) : UserLoginModelResponse {
        Adempiere.getI().startup(false)

		val mapper = ObjectMapper()
		val ctx = Env.getCtx()
		val loginUtil = Login(ctx)
		
		// HACK - this is needed before calling the list of clients, because the user will be logged in
		// HACK - and the information about the login success or failure need to be saved to the DB
		ctx.setProperty(Env.AD_CLIENT_ID, "" + login.clientId)
       	Env.setContext(ctx, Env.AD_CLIENT_ID, "" + login.clientId)
		
		val clients = loginUtil.getClients(login.loginName, login.password)
		if (clients == null) {
			return UserLoginModelResponse()
		}
		
		val selectedClientIndex = clients.indexOfFirst { it.key == login.clientId || clients.count() == 1 }

    	val roles =
    			if (selectedClientIndex == -1 ) {
					null
				} else {
					val clientId = clients[selectedClientIndex].id
					ctx.setProperty(Env.AD_CLIENT_ID, clientId )
			       	Env.setContext(ctx, Env.AD_CLIENT_ID, clientId )
					loginUtil.getRoles(login.loginName, clients[selectedClientIndex] );
				}		
		
    	val user = MUser.get (ctx, login.loginName);
    	if (user != null) {
    		Env.setContext(ctx, Env.AD_USER_ID, user.getAD_User_ID() )
    		Env.setContext(ctx, "#AD_User_Name", user.getName() )
    		Env.setContext(ctx, "#SalesRep_ID", user.getAD_User_ID() )
    	}

		val selectedRoleIndex =
				if (roles==null) { -1 }
				else { roles.indexOfFirst { it.key == login.roleId || roles.count() == 1 } }			
					
		// orgs
		val orgs = 
    			if (selectedRoleIndex == -1 ) {
					null
				} else {
					loginUtil.getOrgs( roles!![selectedRoleIndex] );
				}
		
		val selectedOrgIndex =
				if (orgs==null) { -1 }
				else { orgs.indexOfFirst { it.key == login.orgId || orgs.count() == 1 } }
					
		// warehouses
		val warehouses = 
    			if (selectedOrgIndex == -1 ) {
					null
				} else {
					loginUtil.getWarehouses( orgs!![selectedOrgIndex] );
				}
		
		val selectedWarehouseIndex =
				if (warehouses==null) { -1 }
				else { warehouses.indexOfFirst { it.key == login.warehouseId || warehouses.count() == 1  } }
						
		val AD_User_ID = Env.getAD_User_ID(ctx)
				
		val logged =
			( selectedWarehouseIndex != -1 ) &&
			( selectedOrgIndex != -1 ) &&
			( selectedRoleIndex != -1 ) &&
			( selectedClientIndex != -1 ) &&
			login(
					ctx,
					AD_User_ID,
					roles!![selectedRoleIndex].key,
					clients[selectedClientIndex].key,
					orgs!![selectedOrgIndex].key,
					warehouses!![selectedWarehouseIndex].key,
					login.language )
		
		val result = UserLoginModelResponse( logged, clients, roles, orgs, warehouses, null )
		
		//System.out.println( "LoginManager:" + result.toString() );
	
		return result
	}
}