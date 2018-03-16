package software.hsharp.idempiere.api.servlets.impl

import software.hsharp.business.core.Customers
import software.hsharp.business.models.ICategory
import software.hsharp.business.models.ICustomer
import software.hsharp.business.services.ICustomerResult
import software.hsharp.business.services.ICustomers
import software.hsharp.business.services.ICustomersResult
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

interface ICustomersApi : ICustomers

@Path("customers")
class CustomersEndpoint : ICustomersApi {
    val customers : ICustomers = Customers()

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("all")
    override fun getAllCustomers(): ICustomersResult {
        return customers.getAllCustomers()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    override fun getCustomerById(@PathParam("id") id: Int): ICustomerResult {
        return customers.getCustomerById( id )
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("search")
    override fun getCustomersByAnyCategory(categories: Array<ICategory>): ICustomersResult {
        return customers.getCustomersByAnyCategory( categories )
    }
}
