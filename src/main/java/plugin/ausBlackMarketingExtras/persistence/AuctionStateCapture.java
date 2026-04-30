package plugin.ausBlackMarketingExtras.persistence;

import com.artillexstudios.axdarkauctions.auctions.Auction;
import com.artillexstudios.axdarkauctions.auctions.AuctionManager;
import org.bukkit.entity.Player;
import plugin.ausBlackMarketingExtras.model.CycleSnapshot;

import java.util.Optional;

public final class AuctionStateCapture {

  private AuctionStateCapture() {}

  public static Optional<CycleSnapshot> capture(int cycleId, String auctionName) {
    Auction auction = AuctionManager.getAuctions().get(auctionName);
    if (auction == null || !auction.isRunning()) {
      return Optional.empty();
    }
    Player topBidder = auction.getTopBidder();
    long endTimeEpoch = System.currentTimeMillis() + ((long) auction.getTime() * 50L);
    return Optional.of(new CycleSnapshot(
        cycleId,
        auctionName,
        auction.getState().name(),
        endTimeEpoch,
        auction.getBid(),
        topBidder != null ? topBidder.getUniqueId().toString() : "",
        topBidder != null ? topBidder.getName() : "",
        auction.getBidPage(),
        System.currentTimeMillis()
    ));
  }
}
