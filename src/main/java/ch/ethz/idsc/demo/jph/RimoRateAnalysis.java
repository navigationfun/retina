// code by jph
package ch.ethz.idsc.demo.jph;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.demo.DubendorfHangarLog;
import ch.ethz.idsc.gokart.offline.tab.RimoRateTable;
import ch.ethz.idsc.owl.bot.util.UserHome;
import ch.ethz.idsc.retina.lcm.OfflineLogPlayer;
import ch.ethz.idsc.tensor.io.CsvFormat;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.qty.Quantity;

enum RimoRateAnalysis {
  ;
  private static final File LOG_ROOT = new File("/media/datahaki/media/ethz/gokartlogs");

  public static void main(String[] args) throws IOException {
    // _20180307T150715_28f09b86 ok but needs extraction
    // _20180307T151633_28f09b86 no extraction needed
    // _20180307T154859_0cd18c6b no extraction needed (with stops)
    File file = DubendorfHangarLog._20180307T154859_0cd18c6b.file(LOG_ROOT);
    file = UserHome.file("gokart/pursuit/20180307T154859/log.lcm");
    file = UserHome.file("20180418T132333_bca165ae_prbs4.lcm");
    RimoRateTable rimoRateTable = new RimoRateTable(Quantity.of(0.01, "s"));
    OfflineLogPlayer.process(file, rimoRateTable);
    Export.of(UserHome.file("20180418T132333_bca165ae_prbs4.csv"), rimoRateTable.getTable().map(CsvFormat.strict()));
  }
}
