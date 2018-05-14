package ch.ethz.idsc.gokart.core.perc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ch.ethz.idsc.tensor.Tensor;

public class ClusterCollection {
  private List<ClusterDeque> collection = new ArrayList<>();
  private int i = 0;

  public Tensor toMatrices() {
    return Tensor.of(collection.stream().map(c -> Tensor.of(c.vertexStream())));
  }

  public void maintainUntil(int size) {
    collection.subList(0, size).forEach(ClusterDeque::removeFirst);
    collection = collection.stream().filter(ClusterDeque::nonEmpty).collect(Collectors.toList());
  }

  public List<ClusterDeque> getCollection() {
    return Collections.unmodifiableList(collection);
  }

  public void addToCollection(ClusterDeque clusterDeque) {
    collection.add(clusterDeque);
  }

  public void incrementIDCount() {
    i++;
  }

  public int getIDCount() {
    return i;
  }
}
