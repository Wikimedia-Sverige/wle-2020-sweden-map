package se.wikimedia.service.template.prevayler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.prevayler.Query;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

/**
 * @author Karl Wettin <mailto:karl.wettin@kodapan.se>
 * @since 2019-04-24
 */
@Singleton
@Path("api/prevayler")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class REST {

  @Inject
  private PrevaylerManager prevayler;

  @GET
  @Path("root")
  public Response getRoot() throws Exception {
    return Response.ok().entity(prevayler.execute(new Query<Root, Root>() {
      @Override
      public Root query(Root root, Date executionTime) throws Exception {
        return root;
      }
    })).build();
  }

//  @GET
//  @Path("snapshot")
//  public Response takeSnapshot() throws Exception {
//    return Response.ok().entity(prevayler.takeSnapshot()).build();
//  }

}
