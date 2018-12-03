package ch.ethz.idsc.gokart.core.mpc;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import ch.ethz.idsc.owl.gui.RenderInterface;
import ch.ethz.idsc.owl.gui.win.GeometricLayer;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.mat.LeastSquares;
import ch.ethz.idsc.tensor.red.Norm;

public class TrackLayoutInitialGuess implements RenderInterface {
  private class Cell {
    final int x;
    final int y;
    Scalar cost;
    Boolean inQ = false;
    Boolean processed = false;
    Cell lastCell = null;
    ArrayList<Cell> neighBors = null;

    // ArrayList<Scalar> neighBorCost;
    public Cell(int x, int y) {
      this.x = x;
      this.y = y;
      this.cost = DoubleScalar.POSITIVE_INFINITY;
    }

    public Tensor getPos() {
      return occupancyGrid.getTransform().dot(Tensors.vector(x, y, 1));
    }

    public String toString() {
      return x + " / " + y + " : " + cost + (inQ ? " in Q" : "") + (processed ? " is processed" : "");
    }

    public LinkedList<Cell> getRoute() {
      LinkedList<Cell> result;
      if (lastCell == null)
        result = new LinkedList<>();
      else
        result = lastCell.getRoute();
      result.add(this);
      return result;
    }

    public void findNeighbors() {
      if (neighBors == null) {
        neighBors = new ArrayList<>();
        for (Neighbor n : possibleNeighbors) {
          Cell newNeighBor = n.getFrom(this);
          if (newNeighBor != null) {
            this.neighBors.add(newNeighBor);
            // this.neighBorCost.add(Sqrt.of(RealScalar.of(n.dx^2+n.dy^2)));
          }
        }
      }
    }
  }

  private class Neighbor {
    final int dx;
    final int dy;

    public Neighbor(int dx, int dy) {
      this.dx = dx;
      this.dy = dy;
    }

    public Cell getFrom(Cell cell) {
      int nx = cell.x + dx;
      int ny = cell.y + dy;
      if (!occupancyGrid.isCellOccupied(new Point(nx, ny)))
        return cellGrid[nx][ny];
      else
        return null;
    }
  }

  private final PlanableOccupancyGrid occupancyGrid;

  public TrackLayoutInitialGuess(PlanableOccupancyGrid occupancyGrid) {
    this.occupancyGrid = occupancyGrid;
  }

  // private function for dijkstra
  Cell startingPoint;
  PriorityQueue<Cell> Q;
  int m;
  int n;
  // starting point for Dijkstra
  Cell dijkstraStart;
  // goal for dijkstra
  Cell dijkstraTarget;
  // actual goal
  Cell actualTarget;
  Cell[][] cellGrid;
  ArrayList<Neighbor> possibleNeighbors;
  LinkedList<Cell> route;
  Tensor routePolygon;
  boolean closed = false;

  // this is potentially slow
  Cell getFarthestCell() {
    Cell farthest = dijkstraStart;
    for (int i = 0; i < m; i++) {
      for (int ii = 0; ii < n; ii++) {
        if (cellGrid[i][ii] != null && cellGrid[i][ii].lastCell != null//
            && Scalars.lessThan(farthest.cost, cellGrid[i][ii].cost)) {
          farthest = cellGrid[i][ii];
        }
      }
    }
    return farthest;
  }

  void prepareCells(Tensor gridsize, int startx, int starty, double startorientation) {
    m = gridsize.Get(0).number().intValue();
    n = gridsize.Get(1).number().intValue();
    cellGrid = new Cell[m][n];
    Comparator<Cell> comparator = new Comparator<TrackLayoutInitialGuess.Cell>() {
      @Override
      public int compare(Cell o1, Cell o2) {
        if (Scalars.lessThan(o1.cost, o2.cost))
          return -1;
        else if (o1.cost.equals(o2.cost))
          return 0;
        else
          return 1;
      }
    };
    double dirx = Math.cos(startorientation);
    double diry = Math.sin(startorientation);
    int sfx = (int) Math.round(startx + 2 * dirx);
    int sfy = (int) Math.round(starty + 2 * diry);
    Q = new PriorityQueue<>(comparator);
    // prepare grid
    for (int i = 0; i < gridsize.Get(0).number().intValue(); i++) {
      for (int ii = 0; ii < gridsize.Get(1).number().intValue(); ii++) {
        Cell newCell = new Cell(i, ii);
        if ((i == sfx && ii == sfy) || !occupancyGrid.isCellOccupied(//
            new Point(newCell.x, newCell.y)))
          cellGrid[i][ii] = newCell;
      }
    }
    // add limit at start
    addStartingLine(startx, starty, startorientation);
    // add to Q
    /* Q = new PriorityQueue<>(100000, comparator);
     * for (int i = 0; i < gridsize.Get(0).number().intValue(); i++) {
     * for (int ii = 0; ii < gridsize.Get(1).number().intValue(); ii++) {
     * if (cellGrid[i][ii] != null) {
     * Q.add(cellGrid[i][ii]);
     * cellGrid[i][ii].inQ = true;
     * }
     * }
     * } */
    // add start to Q
    dijkstraStart = cellGrid[sfx][sfy];
    dijkstraStart.cost = RealScalar.ZERO;
    dijkstraStart.inQ = true;
    Q.add(dijkstraStart);
    // add target
    dijkstraTarget = cellGrid[(int) Math.round(startx - 2 * dirx)][(int) Math.round(starty - 2 * diry)];
    // add neighbors
    /* for (int i = 0; i < gridsize.Get(0).number().intValue(); i++) {
     * System.out.println("row: "+i);
     * for (int ii = 0; ii < gridsize.Get(1).number().intValue(); ii++) {
     * if (cellGrid[i][ii] != null)
     * cellGrid[i][ii].findNeighbors();
     * }
     * } */
  }

  void addStartingLine(int startx, int starty, double startorientation) {
    double xforward = Math.cos(startorientation);
    double yforward = Math.sin(startorientation);
    double xsideward = yforward;
    double ysideward = -xforward;
    // find right end
    double currentx = startx;
    double currenty = starty;
    while (currentx >= 0 && currentx < m && currenty >= 0 && currenty < n && cellGrid[(int) currentx][(int) currenty] != null) {
      currentx += xsideward;
      currenty += ysideward;
    }
    int rightx = (int) currentx;
    int righty = (int) currenty;
    // find left end
    currentx = startx;
    currenty = starty;
    while (currentx >= 0 && currentx < m && currenty >= 0 && currenty < n && cellGrid[(int) currentx][(int) currenty] != null) {
      currentx -= xsideward;
      currenty -= ysideward;
    }
    int leftx = (int) currentx;
    int lefty = (int) currenty;
    // delete all cells on line
    int steps = (int) (Math.sqrt(1.0 * (rightx - leftx) * (rightx - leftx) + 1.0 * (righty - lefty) * (righty - lefty)) + 1.0);
    for (int i = 0; i < steps; i++) {
      int posx = (int) Math.round(leftx + (rightx - leftx) * (1.0 * i / (steps - 1.0)));
      int posy = (int) Math.round(lefty + (righty - lefty) * (1.0 * i / (steps - 1.0)));
      if (posx > 0 && posx < m - 1 && posy > 0 && posy < n - 1) {
        // set the neightbors to zero
        cellGrid[posx - 1][posy - 1] = null;
        cellGrid[posx - 1][posy] = null;
        cellGrid[posx - 1][posy + 1] = null;
        cellGrid[posx][posy - 1] = null;
        cellGrid[posx][posy] = null;
        cellGrid[posx][posy + 1] = null;
        cellGrid[posx + 1][posy - 1] = null;
        cellGrid[posx + 1][posy] = null;
        cellGrid[posx + 1][posy + 1] = null;
      }
    }
  }

  void initialise(int x, int y, double orientation) {
    Tensor gridSize = occupancyGrid.getGridSize();
    possibleNeighbors = new ArrayList<>();
    possibleNeighbors.add(new Neighbor(-1, -1));
    possibleNeighbors.add(new Neighbor(-1, 0));
    possibleNeighbors.add(new Neighbor(-1, 1));
    possibleNeighbors.add(new Neighbor(0, -1));
    possibleNeighbors.add(new Neighbor(0, 1));
    possibleNeighbors.add(new Neighbor(1, -1));
    possibleNeighbors.add(new Neighbor(1, 0));
    possibleNeighbors.add(new Neighbor(1, 1));
    prepareCells(gridSize, x, y, orientation);
  }

  public List<Cell> getWayTo(Cell target) {
    LinkedList<Cell> solution = new LinkedList<>();
    solution.add(target);
    while (!solution.getFirst().equals(startingPoint)) {
      solution.addFirst(solution.getFirst().lastCell);
    }
    return solution;
  }

  public void update(int startX, int startY, double startorientation) {
    initialise(startX, startY, startorientation);
    while (!Q.isEmpty()) {
      Cell currentCell = Q.poll();
      currentCell.findNeighbors();
      currentCell.processed = true;
      currentCell.inQ = false;
      for (Cell n : currentCell.neighBors) {
        if (!n.processed) {
          // TODO: maybe use neighborcost (not that important)
          Scalar alternativ = currentCell.cost.add(RealScalar.ONE);
          if (Scalars.lessThan(alternativ, n.cost)) {
            // this could potentially be too slow
            Q.remove(n);
            n.cost = alternativ;
            n.lastCell = currentCell;
            n.inQ = true;
            Q.add(n);
          }
        }
      }
    }
    // check if we can reach target
    if (dijkstraTarget != null && dijkstraTarget.lastCell != null) // we can reach target;
    {
      closed = true;
      actualTarget = dijkstraTarget;
    } else {
      closed = false;
      actualTarget = getFarthestCell();
    }
    route = actualTarget.getRoute();
  }

  public Tensor getRoutePolygon() {
    if (routePolygon == null) {
      routePolygon = Tensors.empty();
      if (route != null) {
        Tensor grid2model = occupancyGrid.getTransform();
        for (Cell c : route) {
          routePolygon.append(grid2model.dot(Tensors.vector(c.x, c.y, 1)));
        }
      }
    }
    return routePolygon;
  }

  public Tensor getControlPointGuess(Scalar spacing, Scalar controlPointResolution) {
    spacing = spacing.multiply(RealScalar.of(m));
    Tensor wantedPositionsX = Tensors.empty();
    Tensor wantedPositionsY = Tensors.empty();
    Tensor lastPosition = route.getFirst().getPos();
    for (Cell c : route) {
      Tensor pos = c.getPos();
      Tensor dist = pos.subtract(lastPosition);
      if (Scalars.lessThan(spacing, Norm._2.of(dist))) {
        lastPosition = pos;
        wantedPositionsX.append(pos.Get(0));
        wantedPositionsY.append(pos.Get(1));
      }
    }
    wantedPositionsX.append(route.getLast().getPos().Get(0));
    wantedPositionsY.append(route.getLast().getPos().Get(1));
    //solve for bspline points
    //number of bspline query points
    int m = wantedPositionsX.length();
    //number of control points
    int n = (int) (wantedPositionsX.length()*controlPointResolution.number().doubleValue());
    //first possible value is 0
    //last possible value is n-2
    Tensor queryPositions = Tensors.vector((i)->RealScalar.of((n-2.0)*(i/(m-1.0))), m);
    //query points
    Tensor splineMatrix = MPCBSpline.getBasisMatrix(n, queryPositions, 0);
    //solve for control points: x
    Tensor controlpointsX = LeastSquares.usingSvd(splineMatrix, wantedPositionsX);
    Tensor controlpointsY = LeastSquares.usingSvd(splineMatrix, wantedPositionsY); 
    return Tensors.of(controlpointsX, controlpointsY);
  }

  @Override
  public void render(GeometricLayer geometricLayer, Graphics2D graphics) {
    Tensor routePolygon = getRoutePolygon();
    Path2D path2d = geometricLayer.toPath2D(routePolygon);
    graphics.draw(path2d);
  }
}
