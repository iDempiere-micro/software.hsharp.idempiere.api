package software.hsharp.idempiere.api.servlets.jwt

import org.idempiere.common.util.KeyNamePair
import software.hsharp.api.helpers.jwt.ILogin
import software.hsharp.api.helpers.jwt.ILoginResponse
import software.hsharp.api.helpers.jwt.IUserLoginModel
import software.hsharp.core.models.INameKeyPair

data class UserLoginModel(
		override val loginName: String,
		override val password: String,
		val clientId : Int,
		val roleId : Int,
		val orgId : Int,
		val warehouseId : Int,
		val language : String )
	: IUserLoginModel, ILogin {
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
		override val logged : Boolean,
		val clients : Array<INameKeyPair>,
		val roles : Array<INameKeyPair>?,
		val orgs :  Array<INameKeyPair>?,
		val warehouses :  Array<INameKeyPair>?,
		override val token : String?
) : ILoginResponse {
    constructor() : this(
			false,
			Array<INameKeyPair>(0, { _ -> KeyNamePair(0,"dummy") } ),
			null,
			null,
			null,
			null
	)
}
