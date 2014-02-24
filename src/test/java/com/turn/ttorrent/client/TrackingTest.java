/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turn.ttorrent.client;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.common.TorrentCreator;
import com.turn.ttorrent.test.TestTorrentMetadataProvider;
import com.turn.ttorrent.test.TorrentTestUtils;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

/**
 * 
 * @author shevek
 */
public class TrackingTest {

	@Test
	public void testTracking() throws Exception {
		Tracker tracker = new Tracker(new InetSocketAddress("localhost", 5674));
		tracker.start();

		try {
			File dir = TorrentTestUtils.newTorrentDir("c_seed");
			TorrentCreator creator = TorrentTestUtils.newTorrentCreator(dir,
					12345678);
			creator.setAnnounce(tracker.getAnnounceUrl().toURI());
			Torrent torrent = creator.create();

			TrackedTorrent trackedTorrent = tracker.announce(torrent);
			trackedTorrent.setAnnounceInterval(1, TimeUnit.MILLISECONDS);

			Client client = new Client(getClass().getSimpleName(),
					new InetSocketAddress("localhost", 6883));
			client.start();

			try {
				final CountDownLatch latch = new CountDownLatch(2);
				TorrentMetadataProvider torrentMetadataProvider = new TestTorrentMetadataProvider(
						torrent.getInfoHash(), tracker.getAnnounceUrl().toURI()) {
					@Override
					public void addPeers(
							Iterable<? extends SocketAddress> peerAddresses) {
						super.addPeers(peerAddresses);
						latch.countDown();
					}
				};
				TrackerHandler trackerHandler = new TrackerHandler(client,
						torrentMetadataProvider);
				trackerHandler.start();

				latch.await(30, TimeUnit.SECONDS);
				assertEquals(0, latch.getCount());

				trackerHandler.stop();
			} finally {
				client.stop();
			}
		} finally {
			tracker.stop();
		}
	}
}
