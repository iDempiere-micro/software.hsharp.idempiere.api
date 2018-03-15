package software.hsharp.idempiere.api.servlets.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.compiere.Adempiere
import org.idempiere.common.util.Env
import software.hsharp.api.helpers.jwt.JwtManager
import software.hsharp.idempiere.api.servlets.Error
import software.hsharp.idempiere.api.servlets.Result
import software.hsharp.idempiere.api.servlets.jwt.LoginManager
import software.hsharp.idempiere.api.servlets.jwt.UserLoginModel
import software.hsharp.idempiere.api.servlets.jwt.UserLoginModelResponse
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.security.PermitAll
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@PermitAll
@Path("authentication")
class LoginUser {

	protected fun doLogin( username : String, password : String) : UserLoginModelResponse {
		val mapper = ObjectMapper()

		Adempiere.getI().startup(false)

		val loginManager = LoginManager()
		val userLoginModel = UserLoginModel(username, password)

		val result = loginManager.doLogin( userLoginModel )
		if (result.logged) {
			val AD_User_ID = Env.getAD_User_ID(Env.getCtx())
			val token = JwtManager.createToken( AD_User_ID.toString(), "", mapper.writeValueAsString(userLoginModel) )
			return result.copy(token=token)
		} else {
			return result
		}

	}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.TEXT_PLAIN)
    fun login1(
		@QueryParam("username") username : String,
		@QueryParam("password") password : String
	) : String {
		try {
			val mapper = ObjectMapper()
			return mapper.writeValueAsString(doLogin(username, password));
		} catch (e:Exception) {
			val sw = StringWriter()
			e.printStackTrace(PrintWriter(sw))
			
			val result = Result( Error ( "Login failed:" + e.toString() + "[" + sw.toString() + "]" ) )
			return result.toString();
		}
	}


    @POST
    @Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun login2(
		@FormParam("username") username : String,
		@FormParam("password") password : String
	) : String {
		try {
			val mapper = ObjectMapper()
			return mapper.writeValueAsString(doLogin(username, password));
		} catch (e:Exception) {
			val sw = StringWriter()
			e.printStackTrace(PrintWriter(sw))
			
			val result = Result( Error ( "Login failed:" + e.toString() + "[" + sw.toString() + "]" ) )
			return result.toString();
		}
												
	}
}

/* Sample POST body to test
 		
{ "userName":"System", "password":"System", "clientId" : 1000002, "roleId":1000004, "orgId":1000002, "warehouseId" : 1000000 }
  		
*/

