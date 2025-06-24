package dev.mars.resource;

import dev.mars.domain.Counterparty;
import dev.mars.dto.CounterpartyDto;
import dev.mars.dto.CreateCounterpartyRequest;
import dev.mars.service.CounterpartyService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/api/counterparties")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CounterpartyResource {

    private static final Logger LOG = Logger.getLogger(CounterpartyResource.class);

    @Inject
    CounterpartyService counterpartyService;

    @GET
    public Response getAllCounterparties(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("type") Counterparty.CounterpartyType type,
            @QueryParam("status") Counterparty.CounterpartyStatus status,
            @QueryParam("search") String search) {
        
        LOG.debugf("GET /api/counterparties - page: %d, size: %d, type: %s, status: %s, search: %s", 
                   page, size, type, status, search);

        List<CounterpartyDto> counterparties;

        if (search != null && !search.trim().isEmpty()) {
            counterparties = counterpartyService.searchCounterpartiesByName(search.trim());
        } else if (type != null) {
            counterparties = counterpartyService.getCounterpartiesByType(type);
        } else if (status != null && status == Counterparty.CounterpartyStatus.ACTIVE) {
            counterparties = counterpartyService.getActiveCounterparties();
        } else if (page > 0 || size != 20) {
            counterparties = counterpartyService.getAllCounterpartiesPaged(page, size);
        } else {
            counterparties = counterpartyService.getAllCounterparties();
        }

        return Response.ok(counterparties).build();
    }

    @GET
    @Path("/{id}")
    public Response getCounterpartyById(@PathParam("id") Long id) {
        LOG.debugf("GET /api/counterparties/%d", id);
        
        return counterpartyService.getCounterpartyById(id)
                .map(counterparty -> Response.ok(counterparty).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/code/{code}")
    public Response getCounterpartyByCode(@PathParam("code") String code) {
        LOG.debugf("GET /api/counterparties/code/%s", code);
        
        return counterpartyService.getCounterpartyByCode(code)
                .map(counterparty -> Response.ok(counterparty).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/active")
    public Response getActiveCounterparties() {
        LOG.debug("GET /api/counterparties/active");
        
        List<CounterpartyDto> activeCounterparties = counterpartyService.getActiveCounterparties();
        return Response.ok(activeCounterparties).build();
    }

    @POST
    public Response createCounterparty(@Valid CreateCounterpartyRequest request) {
        LOG.debugf("POST /api/counterparties - creating counterparty with code: %s", request.code);
        
        CounterpartyDto created = counterpartyService.createCounterparty(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateCounterparty(@PathParam("id") Long id, @Valid CreateCounterpartyRequest request) {
        LOG.debugf("PUT /api/counterparties/%d", id);
        
        CounterpartyDto updated = counterpartyService.updateCounterparty(id, request);
        return Response.ok(updated).build();
    }

    @PATCH
    @Path("/{id}/status")
    public Response updateCounterpartyStatus(@PathParam("id") Long id, 
                                           @QueryParam("status") Counterparty.CounterpartyStatus status) {
        LOG.debugf("PATCH /api/counterparties/%d/status - status: %s", id, status);
        
        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Status parameter is required")
                    .build();
        }
        
        CounterpartyDto updated = counterpartyService.updateCounterpartyStatus(id, status);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCounterparty(@PathParam("id") Long id) {
        LOG.debugf("DELETE /api/counterparties/%d", id);
        
        counterpartyService.deleteCounterparty(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/stats/count")
    public Response getCounterpartyCount() {
        LOG.debug("GET /api/counterparties/stats/count");
        
        long totalCount = counterpartyService.getCounterpartyCount();
        long activeCount = counterpartyService.getActiveCounterpartyCount();
        
        return Response.ok(new CounterpartyStats(totalCount, activeCount)).build();
    }

    public static class CounterpartyStats {
        public long totalCount;
        public long activeCount;

        public CounterpartyStats() {
        }

        public CounterpartyStats(long totalCount, long activeCount) {
            this.totalCount = totalCount;
            this.activeCount = activeCount;
        }
    }
}
