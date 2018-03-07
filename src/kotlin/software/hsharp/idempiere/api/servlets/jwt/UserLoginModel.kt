package software.hsharp.idempiere.api.servlets.jwt

import org.idempiere.common.util.KeyNamePair
import software.hsharp.api.helpers.jwt.IUserLoginModel

data class UserLoginModel(
		val userName: String,
		val password: String,
		val clientId : Int,
		val roleId : Int,
		val orgId : Int,
		val warehouseId : Int,
		val language : String )
	: IUserLoginModel {
    constructor() : this("", 
                         "",
                         0, //System ClientId = 0
                         -1,
                         -1,
                         -1,
                         "en_US")
						 
    constructor(userName: String, password: String) : this(userName, 
                         password,
                         0, //System ClientId = 0
                         -1,
                         -1,
                         -1,
                         "en_US"
	)
}

data class UserLoginModelResponse(
		val logged : Boolean,
		val clients : Array<KeyNamePair>,
		val roles : Array<KeyNamePair>?,
		val orgs :  Array<KeyNamePair>?,
		val warehouses :  Array<KeyNamePair>?,
		val token : String?
)
{
    constructor() : this(
			false,
			Array<KeyNamePair>(0, { _ -> KeyNamePair(0,"dummy") } ),
			null,
			null,
			null,
			null
	)
}
