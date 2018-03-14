package software.hsharp.idempiere.api.servlets.impl

import software.hsharp.business.core.Customers
import software.hsharp.business.models.ICategory
import software.hsharp.business.models.ICustomer
import software.hsharp.business.services.ICustomers
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

interface ICustomersApi : ICustomers

@Path("customers")
class CustomersEndpoint : ICustomersApi {
    val customers : ICustomers = Customers()

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("all")
    override fun getAllCustomers(): Array<ICustomer> {
        return customers.getAllCustomers()
    }

    override fun getCustomerById(id: Int): ICustomer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCustomersByAnyCategory(categories: Array<ICategory>): Array<ICustomer> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
