// code by jph
package ch.ethz.idsc.retina.demo.jph.lidar;

import ch.ethz.idsc.retina.dev.lidar.app.VelodynePcapPacketListener;
import ch.ethz.idsc.retina.dev.lidar.vlp16.Vlp16SpacialProvider;
import ch.ethz.idsc.retina.util.io.ByteArrayConsumer;
import ch.ethz.idsc.retina.util.io.PcapParse;

enum PcapParseDemo {
  ;
  public static void main(String[] args) throws Exception {
    @SuppressWarnings("unused")
    ByteArrayConsumer byteArrayConsumer = new ByteArrayConsumer() {
      @Override
      public void accept(byte[] packet_data, int length) {
        System.out.println("" + length);
      }
    };
    VelodynePcapPacketListener velodynePcapPacketListener = VelodynePcapPacketListener.vlp16();
    Vlp16SpacialProvider vlp16SpacialProvider = new Vlp16SpacialProvider();
    velodynePcapPacketListener.velodyneDecoder.addRayListener(vlp16SpacialProvider);
    PcapParse.of(Vlp16Pcap.DOWNTOWN_SINGLE.file, velodynePcapPacketListener);
  }
}