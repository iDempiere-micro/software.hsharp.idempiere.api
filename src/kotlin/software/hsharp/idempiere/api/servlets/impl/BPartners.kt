package software.hsharp.idempiere.api.servlets.impl

import org.compiere.model.MBPartner
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import software.hsharp.business.core.BusinessPartners
import software.hsharp.business.models.IBusinessPartner
import software.hsharp.business.services.IBusinessPartners
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

interface IBusinessPartnersApi : IBusinessPartners

@Path("bpartners")
class BPartners : IBusinessPartnersApi {

    val bPartners : IBusinessPartners = BusinessPartners()

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("all")
    override fun getAllBusinessPartners() : Array<IBusinessPartner> {
        return bPartners.getAllBusinessPartners()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    override fun getBusinessPartnerById( @PathParam("id") id : Int ) : IBusinessPartner? {
        return bPartners.getBusinessPartnerById( id )
    }
}
