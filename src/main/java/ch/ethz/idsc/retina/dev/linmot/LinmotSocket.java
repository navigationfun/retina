// code by jph
package ch.ethz.idsc.retina.dev.linmot;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ch.ethz.idsc.retina.dev.zhkart.AutoboxSocket;
import ch.ethz.idsc.retina.util.io.DatagramSocketManager;

/**  */
public class LinmotSocket extends AutoboxSocket<LinmotGetListener, LinmotPutEvent, LinmotPutProvider> {
  public static final LinmotSocket INSTANCE = new LinmotSocket();
  // ---
  private static final int LOCAL_PORT = 5001;
  private static final String LOCAL_ADDRESS = "192.168.1.1";
  // ---
  private static final int REMOTE_PORT = 5001;
  private static final String REMOTE_ADDRESS = "192.168.1.10";
  // ---
  private static final int SEND_PERIOD_MS = 20;
  // ---

  private LinmotSocket() {
    super(DatagramSocketManager.local(new byte[LinmotGetEvent.LENGTH], LinmotSocket.LOCAL_PORT, LinmotSocket.LOCAL_ADDRESS));
    // ---
    addProvider(LinmotCalibrationProvider.INSTANCE);
    addProvider(LinmotPutFallback.INSTANCE);
  }

  @Override
  public void accept(byte[] data, int length) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    LinmotGetEvent linmotGetEvent = new LinmotGetEvent(byteBuffer);
    for (LinmotGetListener listener : listeners)
      try {
        listener.linmotGet(linmotGetEvent);
      } catch (Exception exception) {
        exception.printStackTrace();
      }
  }

  @Override
  protected long getPeriod() {
    return SEND_PERIOD_MS;
  }

  @Override
  protected DatagramPacket getDatagramPacket(LinmotPutEvent linmotPutEvent) throws UnknownHostException {
    byte[] data = new byte[LinmotPutEvent.LENGTH];
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    linmotPutEvent.insert(byteBuffer);
    return new DatagramPacket(data, data.length, //
        InetAddress.getByName(LinmotSocket.REMOTE_ADDRESS), LinmotSocket.REMOTE_PORT);
  }
}
