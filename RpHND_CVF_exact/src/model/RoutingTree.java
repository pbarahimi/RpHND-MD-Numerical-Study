package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class RoutingTree {
	public Route[] routes ;
	public HashMap<Integer,ArrayList<Node>> usedHubs = new HashMap<Integer, ArrayList<Node>>();
	public HashMap<Integer,ArrayList<Route>> availableRoutes = new HashMap<Integer, ArrayList<Route>>();
	public ArrayList<Integer> unexploredNodes = new ArrayList<Integer>(); 
	public double value;
	public boolean complete = false; // once the length of the routes attribute reaches the maximum number nodes in a tree (either with routes or null values as nodes), the routing tree is complete.
	public boolean pruned = false;  // if the value of the tree is worse than the upper bound, the tree will be pruned eventhough it might not be complete.
	
	// Constructor 0
	public RoutingTree(int size){
		this.routes = new Route[size];
	}
	
	// Constructor 1
	@SuppressWarnings("unchecked")
	public RoutingTree (Route[] routes, Route newRoute, ArrayList<Route> avlRoutes, int L) {
		this.routes = routes.clone();
		this.unexploredNodes.add(0);
		this.availableRoutes.put(0, (ArrayList<Route>) avlRoutes.clone() );
		this.usedHubs.put( 0, new ArrayList<Node>() );
		addRoute(newRoute);
		updateValue();
		
		// check if the tree is completed, according to the required depth.
		if ( this.unexploredNodes.isEmpty() || this.routes.length >= Math.pow(2, L+1)-2 )
			this.complete = true;
	};
	
	// Constructor 2
	public RoutingTree (RoutingTree rt, int newNodeInd, Route newRoute, ArrayList<Route> avlRoutes, int L){
		this.routes = rt.routes.clone();
		this.usedHubs.putAll(rt.usedHubs);
		this.availableRoutes.putAll(rt.availableRoutes);
		this.unexploredNodes.addAll(rt.unexploredNodes);	
		this.availableRoutes.put(newNodeInd, new ArrayList<Route>(avlRoutes) );
		updateUsedHubs(newNodeInd);
		addRoute(newRoute);
		updateValue();
		
		// check if the tree is completed, according to the required depth.
		if ( this.unexploredNodes.isEmpty() || this.routes.length >= Math.pow(2, L+1)-2 )
			this.complete = true;
	}
	
	private void updateUsedHubs(int newNodeInd){
		// Adding parent node's used hubs list to the new node.
		int parentNodeInd = (int) Math.floor((newNodeInd - 1)/2);
		this.usedHubs.put(newNodeInd, new ArrayList<Node>(this.usedHubs.get(parentNodeInd)) );
		
		if (newNodeInd % 2 == 0) // if new node is a right child
			// add the second hub of the parent node
			this.usedHubs.get(newNodeInd).add(this.routes[parentNodeInd].m);
		else // if new node is a left node
			// add the first hub of the parent node
			this.usedHubs.get(newNodeInd).add(this.routes[parentNodeInd].k);
	}
	
	private void addRoute(Route r){
		int index = unexploredNodes.get(0);
		unexploredNodes.remove(0);
		availableRoutes.get(index).remove(r);
		this.routes[index] = r;
		/*if (index == 0)
			this.routes.add(r);
		else{
			try{
				this.routes[index] = r;
			}catch(IndexOutOfBoundsException e){
				for (int i = this.routes.size() ; i < index ; i++)
					this.routes.add(null);
				this.routes.add(r);
			}
		}*/
		
		// See the index to find the left and right child indices and add to the unexplored list. 		
		// check whether the new route needs backups
		if ( !r.k.equals(r.m) ) {
			if ( r.i.equals(r.k) && !r.j.equals(r.m)) //iimj
				this.unexploredNodes.add(2*index+2);
			else if ( !r.i.equals(r.k) && r.j.equals(r.m) )  //ikjj
				this.unexploredNodes.add(2*index+1);
			else if ( !r.i.equals(r.k) && !r.j.equals(r.m) ){ //ikmj
				this.unexploredNodes.add(2*index+1);
				this.unexploredNodes.add(2*index+2);
			}
			else {} //iijj
			
		} else {
			if ( !r.i.equals(r.k) && !r.j.equals(r.m) ) //ikkj
				this.unexploredNodes.add(2*index+1);
			else {} //iiij or ijjj
		}
	}
	
	public RoutingTree (RoutingTree other) {
		this.routes = other.routes;
		this.usedHubs = other.usedHubs;
		this.value = other.value;
		this.availableRoutes = other.availableRoutes;
		this.complete = other.complete;
	};
	
	public void updateValue(){		
		this.value = this.routes[0].expCost;
		for (int i = 1 ; i < routes.length ; i++){
			if (this.routes[i] != null)
				this.value += this.routes[i].cost * getFailureProb(i);
		}	
	}
	
	private double getFailureProb(int i ){
		double output = 1;
		for ( Node n : this.usedHubs.get(i) )
			output *= n.failure;
		return output;
	}
	
	public static ArrayList<Route> getFeasibleRoutes(
			Node i, 
			Node j, 
			List<Node> hList,
			Route[][][][] routes,
			double[][] distances,
			double alpha
			){
		// generating list of feasible routes between the origin and the
				// destination.
				ArrayList<Route> feasibleRoutes = new ArrayList<Route>();
				if (i.isHub && j.isHub) {
					if (routes[i.ID][i.ID][j.ID][j.ID] == null)
						routes[i.ID][i.ID][j.ID][j.ID] = new Route(i, i, j, j,
								distances, alpha);
					feasibleRoutes.add(routes[i.ID][i.ID][j.ID][j.ID]);
				} else if (i.isHub) {
					for (Node n : hList) {
						if (routes[i.ID][i.ID][n.ID][j.ID] == null)
							routes[i.ID][i.ID][n.ID][j.ID] = new Route(i, i, n, j,
									distances, alpha);
						feasibleRoutes.add(routes[i.ID][i.ID][n.ID][j.ID]);
					}
				} else if (j.isHub) {
					for (Node n : hList) {
						if (routes[i.ID][n.ID][j.ID][j.ID] == null)
							routes[i.ID][n.ID][j.ID][j.ID] = new Route(i, n, j, j,
									distances, alpha);
						feasibleRoutes.add(routes[i.ID][n.ID][j.ID][j.ID]);
					}
				} else {
					for (int u = 0; u < hList.size(); u++) {
						for (int v = u; v < hList.size(); v++) {
							if (routes[i.ID][hList.get(u).ID][hList.get(v).ID][j.ID] == null
									&& routes[i.ID][hList.get(v).ID][hList.get(u).ID][j.ID] == null) {
								Route r1 = new Route(i, hList.get(u), hList.get(v), j,
										distances, alpha);
								Route r2 = new Route(i, hList.get(v), hList.get(u), j,
										distances, alpha);
								if (r1.value <= r2.value) {
									routes[r1.i.ID][r1.k.ID][r1.m.ID][r1.j.ID] = r1;
									feasibleRoutes.add(r1);
								} else {
									routes[r2.i.ID][r2.k.ID][r2.m.ID][r2.j.ID] = r2;
									feasibleRoutes.add(r2);
								}
							} else if (routes[i.ID][hList.get(u).ID][hList.get(v).ID][j.ID] != null) {
								feasibleRoutes.add(routes[i.ID][hList.get(u).ID][hList
										.get(v).ID][j.ID]);
							} else /*
									 * if (
									 * !routes[i.ID][hList.get(v).ID][hList.get(u).ID
									 * ][j.ID].equals(null) )
									 */{
								feasibleRoutes.add(routes[i.ID][hList.get(v).ID][hList
										.get(u).ID][j.ID]);
							}
						}
					}
				}
				return feasibleRoutes;
	}
	
	public static PriorityQueue<Route> getFeasibleRoutes2(
			Node i, 
			Node j, 
			List<Node> hList,
			Route[][][][] routes,
			double[][] distances,
			double alpha
			){
		// generating list of feasible routes between the origin and the
				// destination.
		PriorityQueue<Route> feasibleRoutes = new PriorityQueue<Route>();
				if (i.isHub && j.isHub) {
					if (routes[i.ID][i.ID][j.ID][j.ID] == null)
						routes[i.ID][i.ID][j.ID][j.ID] = new Route(i, i, j, j,
								distances, alpha);
					feasibleRoutes.add(routes[i.ID][i.ID][j.ID][j.ID]);
				} else if (i.isHub) {
					for (Node n : hList) {
						if (routes[i.ID][i.ID][n.ID][j.ID] == null)
							routes[i.ID][i.ID][n.ID][j.ID] = new Route(i, i, n, j,
									distances, alpha);
						feasibleRoutes.add(routes[i.ID][i.ID][n.ID][j.ID]);
					}
				} else if (j.isHub) {
					for (Node n : hList) {
						if (routes[i.ID][n.ID][j.ID][j.ID] == null)
							routes[i.ID][n.ID][j.ID][j.ID] = new Route(i, n, j, j,
									distances, alpha);
						feasibleRoutes.add(routes[i.ID][n.ID][j.ID][j.ID]);
					}
				} else {
					for (int u = 0; u < hList.size(); u++) {
						for (int v = u; v < hList.size(); v++) {
							if (routes[i.ID][hList.get(u).ID][hList.get(v).ID][j.ID] == null
									&& routes[i.ID][hList.get(v).ID][hList.get(u).ID][j.ID] == null) {
								Route r1 = new Route(i, hList.get(u), hList.get(v), j,
										distances, alpha);
								Route r2 = new Route(i, hList.get(v), hList.get(u), j,
										distances, alpha);
								if (r1.value <= r2.value) {
									routes[r1.i.ID][r1.k.ID][r1.m.ID][r1.j.ID] = r1;
									feasibleRoutes.add(r1);
								} else {
									routes[r2.i.ID][r2.k.ID][r2.m.ID][r2.j.ID] = r2;
									feasibleRoutes.add(r2);
								}
							} else if (routes[i.ID][hList.get(u).ID][hList.get(v).ID][j.ID] != null) {
								feasibleRoutes.add(routes[i.ID][hList.get(u).ID][hList
										.get(v).ID][j.ID]);
							} else /*
									 * if (
									 * !routes[i.ID][hList.get(v).ID][hList.get(u).ID
									 * ][j.ID].equals(null) )
									 */{
								feasibleRoutes.add(routes[i.ID][hList.get(v).ID][hList
										.get(u).ID][j.ID]);
							}
						}
					}
				}
				return feasibleRoutes;
	}
	/**
	 * returns a list of routes that do not contain node h as hub.
	 * @param avlRoutes - initial list of routes.
	 * @param h - the node to be removed from the routes.
	 * @return
	 */
	public static ArrayList<Route> getAvlRoutes(ArrayList<Route> avlRoutes, Node h){
		ArrayList<Route> output = new ArrayList<Route>();
		for (Route r : avlRoutes)
			if (!r.k.equals(h) && !r.m.equals(h))
				output.add(r);
		return output;
	}
	
	public String toString(){
		String output = "";
		for (int i = 0 ; i < routes.length ; i++){
			if (routes[i] != null )
				output = output.concat(i + ") " + routes[i] + "\r\n");
		}
		output = output.concat("value: " + this.value);
		return output;
	}
}