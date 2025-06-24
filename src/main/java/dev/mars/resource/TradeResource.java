package dev.mars.resource;

import dev.mars.domain.Trade;
import dev.mars.dto.CreateTradeRequest;
import dev.mars.dto.TradeDto;
import dev.mars.service.TradeService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Path("/api/trades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TradeResource {

    private static final Logger LOG = Logger.getLogger(TradeResource.class);

    @Inject
    TradeService tradeService;

    @GET
    public Response getAllTrades(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("counterpartyId") Long counterpartyId,
            @QueryParam("status") Trade.TradeStatus status,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("recent") @DefaultValue("0") int recentDays) {
        
        LOG.debugf("GET /api/trades - page: %d, size: %d, counterpartyId: %s, status: %s", 
                   page, size, counterpartyId, status);

        List<TradeDto> trades;

        if (recentDays > 0) {
            trades = tradeService.getRecentTrades(recentDays);
        } else if (counterpartyId != null) {
            trades = tradeService.getTradesByCounterpartyId(counterpartyId);
        } else if (status != null) {
            trades = tradeService.getTradesByStatus(status);
        } else if (startDate != null && endDate != null) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            trades = tradeService.getTradesByDateRange(start, end);
        } else if (page > 0 || size != 20) {
            trades = tradeService.getAllTradesPaged(page, size);
        } else {
            trades = tradeService.getAllTrades();
        }

        return Response.ok(trades).build();
    }

    @GET
    @Path("/{id}")
    public Response getTradeById(@PathParam("id") Long id) {
        LOG.debugf("GET /api/trades/%d", id);
        
        return tradeService.getTradeById(id)
                .map(trade -> Response.ok(trade).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/reference/{reference}")
    public Response getTradeByReference(@PathParam("reference") String reference) {
        LOG.debugf("GET /api/trades/reference/%s", reference);
        
        return tradeService.getTradeByReference(reference)
                .map(trade -> Response.ok(trade).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/pending")
    public Response getPendingTrades() {
        LOG.debug("GET /api/trades/pending");
        
        List<TradeDto> pendingTrades = tradeService.getPendingTrades();
        return Response.ok(pendingTrades).build();
    }

    @GET
    @Path("/counterparty/{counterpartyId}")
    public Response getTradesByCounterparty(@PathParam("counterpartyId") Long counterpartyId) {
        LOG.debugf("GET /api/trades/counterparty/%d", counterpartyId);
        
        List<TradeDto> trades = tradeService.getTradesByCounterpartyId(counterpartyId);
        return Response.ok(trades).build();
    }

    @POST
    public Response createTrade(@Valid CreateTradeRequest request) {
        LOG.debugf("POST /api/trades - creating trade with reference: %s", request.tradeReference);
        
        TradeDto created = tradeService.createTrade(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateTrade(@PathParam("id") Long id, @Valid CreateTradeRequest request) {
        LOG.debugf("PUT /api/trades/%d", id);
        
        TradeDto updated = tradeService.updateTrade(id, request);
        return Response.ok(updated).build();
    }

    @PATCH
    @Path("/{id}/status")
    public Response updateTradeStatus(@PathParam("id") Long id, 
                                    @QueryParam("status") Trade.TradeStatus status) {
        LOG.debugf("PATCH /api/trades/%d/status - status: %s", id, status);
        
        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Status parameter is required")
                    .build();
        }
        
        TradeDto updated = tradeService.updateTradeStatus(id, status);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTrade(@PathParam("id") Long id) {
        LOG.debugf("DELETE /api/trades/%d", id);
        
        tradeService.deleteTrade(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/stats/count")
    public Response getTradeStats() {
        LOG.debug("GET /api/trades/stats/count");
        
        long totalCount = tradeService.getTradeCount();
        long pendingCount = tradeService.getTradeCountByStatus(Trade.TradeStatus.PENDING);
        long confirmedCount = tradeService.getTradeCountByStatus(Trade.TradeStatus.CONFIRMED);
        long settledCount = tradeService.getTradeCountByStatus(Trade.TradeStatus.SETTLED);
        
        return Response.ok(new TradeStats(totalCount, pendingCount, confirmedCount, settledCount)).build();
    }

    @GET
    @Path("/stats/value/{counterpartyId}")
    public Response getTotalTradeValueByCounterparty(@PathParam("counterpartyId") Long counterpartyId) {
        LOG.debugf("GET /api/trades/stats/value/%d", counterpartyId);
        
        BigDecimal totalValue = tradeService.getTotalTradeValueByCounterparty(counterpartyId);
        return Response.ok(new TradeValueStats(counterpartyId, totalValue)).build();
    }

    public static class TradeStats {
        public long totalCount;
        public long pendingCount;
        public long confirmedCount;
        public long settledCount;

        public TradeStats() {
        }

        public TradeStats(long totalCount, long pendingCount, long confirmedCount, long settledCount) {
            this.totalCount = totalCount;
            this.pendingCount = pendingCount;
            this.confirmedCount = confirmedCount;
            this.settledCount = settledCount;
        }
    }

    public static class TradeValueStats {
        public Long counterpartyId;
        public BigDecimal totalValue;

        public TradeValueStats() {
        }

        public TradeValueStats(Long counterpartyId, BigDecimal totalValue) {
            this.counterpartyId = counterpartyId;
            this.totalValue = totalValue;
        }
    }
}
