/*
 * ClusterLayout.java
 *
 * Created on October 1, 2008, 2:14 PM
 *
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.layout;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;
import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.dataset.*;

public class ClusterLayout extends Layout {
	public static String DEFAULT_METADATA_LABEL = "default";
	public static final int MINX_RADIUS = VueResources
			.getInt("layout.minx_radius");
	public static final int MINY_RADIUS = VueResources
			.getInt("layout.miny_radius");
	public static final int X_SPACING = VueResources.getInt("layout.x_spacing");
	public static final int Y_SPACING = VueResources.getInt("layout.y_spacing");
	public static final double FACTOR = VueResources
			.getDouble("layout.space_ratio");
	public static final int MAX_COLLISION_CHECK = VueResources
			.getInt("layout.check_overlap_number");
	public final int clusterColumn = 3;
	public final int total = 15;

	/** Creates a new instance of ClusterLayout */
	public ClusterLayout() {
	}

	public LWMap createMap(Dataset ds, String mapName) throws Exception {

		Map<String, LWNode> nodeMap = new HashMap<String, LWNode>();
		Map<String, Integer> repeatMap = new HashMap<String, Integer>();
		ArrayList<String> clusterColumnList = new ArrayList<String>();
		LWMap map = new LWMap(mapName);
		int count = 0;
		// set map size of the map
		double rowCount = ds.getRowList().size();
		double goodSize = (int) Math.sqrt(rowCount) * 100;
		MAP_SIZE = MAP_SIZE > goodSize ? MAP_SIZE : goodSize;

		for (ArrayList<String> row : ds.getRowList()) {
			String node1Label = row.get(0);
			LWNode node1;
			node1 = new LWNode(node1Label);
			for (int i = 1; i < row.size(); i++) {
				String value = row.get(i);
				String key = ((ds.getHeading() == null) || ds.getHeading()
						.size() < i) ? DEFAULT_METADATA_LABEL : ds.getHeading()
						.get(i);
				// System.out.println("i="+i+" key="+key+" value ="+value);

				VueMetadataElement vm = new VueMetadataElement();
				vm.setKey(key);
				vm.setValue(value);
				vm.setType(VueMetadataElement.CATEGORY);
				node1.getMetadataList().addElement(vm);
			}
			if (ds.getHeading().size() > 1
					&& ds.getHeading().get(1).equals("resource")) {
				Resource resource = node1.getResourceFactory().get(
						new File(row.get(1)));
				node1.setResource(resource);
			}
			// special hack to demo the dataset laurie baise dataset
			if (ds.getHeading().size() > 6
					&& ds.getHeading().get(6).equals("Actual")) {
				if (row.get(6).equalsIgnoreCase("A")) {
					node1.setFillColor(Color.CYAN);
				}
			}
			if (ds.getHeading().size() > 2
					&& ds.getHeading().get(2).equals("Role")) {
				if (row.get(2).contains("mentor")) {
					node1.setFillColor(Color.CYAN);
				}
			}
			if (ds.getHeading().size() > 9
					&& ds.getHeading().get(9).contains("Total")) {
				double width = Double.parseDouble(row.get(9));
				width = Math.sqrt(width);
				if (width > 10)
					width = 10;
				node1.setStrokeWidth((float) width);

			}
			String clusterElement = row.get(clusterColumn);
			if (!clusterColumnList.contains(clusterElement))
				clusterColumnList.add(clusterElement);
			nodeMap.put(node1Label, node1);
			int COLUMNS = 8;
			int MAP_SIZE = 5000;
			double Q_SIZE = (double) MAP_SIZE / COLUMNS;
			double x = (clusterColumnList.indexOf(clusterElement) % COLUMNS)
					* Q_SIZE - Q_SIZE / 2;
			double y = (clusterColumnList.indexOf(clusterElement) / COLUMNS)
					* Q_SIZE - Q_SIZE / 2;

			node1.layout();
			map.add(node1);

			double angle = Math.random() * Math.PI * 4;

			node1.setLocation(x + Math.cos(angle) * Q_SIZE / 3, y
					+ Math.sin(angle) * Q_SIZE / 3);

			// node1.setLocation(MAP_SIZE*Math.random(),MAP_SIZE*Math.random());

		}
		return map;
	}

	public void layout(LWSelection selection) {
		System.out.println("Applying the cluster layout");
		HashMap<LWComponent, ArrayList<LWComponent>> clusterMap = new HashMap<LWComponent, ArrayList<LWComponent>>();
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxNodeWidth = X_COL_SIZE;
		double maxNodeHeight = Y_COL_SIZE;
		int count = 0;
		int total = 0;
		int mod = 4;
		Iterator<LWComponent> i = VUE.getActiveMap().getAllDescendents(
				LWContainer.ChildKind.PROPER).iterator();
		// placing the cluster nodes in a map with the center node as a key
		while (i.hasNext()) {
			LWComponent c = i.next();
			if (c instanceof LWLink) {
				LWLink link = (LWLink) c;
				LWComponent head = link.getHead();
				LWComponent tail = link.getTail();
				if (selection.contains(head)) {
					if (!clusterMap.containsKey(head)) {
						clusterMap.put(head, new ArrayList<LWComponent>());
					}
					clusterMap.get(head).add(tail);
				}
				if (selection.contains(tail)) {
					if (!clusterMap.containsKey(tail)) {
						clusterMap.put(tail, new ArrayList<LWComponent>());
					}
					clusterMap.get(tail).add(head);
				}
			} else if (c instanceof LWNode) {
				maxNodeWidth = maxNodeWidth > c.getWidth() ? maxNodeWidth : c
						.getWidth();
				maxNodeHeight = maxNodeHeight > c.getHeight() ? maxNodeHeight
						: c.getHeight();
			}

		}
		// computing the minimum and X and Y position of selection
		// TODO: use the center of selection to compute min and max instead
		Iterator<LWComponent> iter = selection.iterator();
		while (iter.hasNext()) {
			LWComponent c = iter.next();
			if (c instanceof LWNode) {
				LWNode node = (LWNode) c;
				minX = node.getLocation().getX() < minX ? node.getLocation()
						.getX() : minX;
				minY = node.getLocation().getY() < minY ? node.getLocation()
						.getY() : minY;
				total++;
			}
		}

		// computing the size of largest cluster
		int maxClusterSize = 10; // default size is zero
		for (LWComponent c : clusterMap.keySet()) {
			int clusterSize = clusterMap.get(c).size();
			if (clusterSize > maxClusterSize)
				maxClusterSize = clusterSize;
		}
//		double maxRadius = Math.sqrt(FACTOR * maxClusterSize * maxNodeWidth * maxNodeHeight / Math.PI);
		double x = minX;
		double y = minY;
		mod = (int) Math.ceil(Math.sqrt((double) total));
        //TODO: need to place the central nodes better
		iter = selection.iterator();
		// making the clusters
		while (iter.hasNext()) {
			LWComponent c = iter.next();
			if (c instanceof LWNode) {
				
				LWNode node = (LWNode) c;
				double radius = Math.sqrt(FACTOR * clusterMap.get(node).size()* maxNodeWidth * maxNodeHeight / Math.PI);
				
				// int totalLinked = clusterMap.get(node).size();
				total++;
				if (count % mod == 0) {
					if (count != 0) {
						double increment = 2 * (radius+maxNodeHeight+Y_SPACING);  
						y += increment;
					}
					x = minX;
				} else {
					double increment = 2 * (radius+maxNodeWidth+X_SPACING);
					x += increment;
				}
				count++;
				double nodeWidth = node.getWidth();
				double nodeHeight = node.getHeight();
				node.setLocation(x - nodeWidth / 2, y - nodeHeight / 2);
//				System.out.println("Placed node: " + node.getLabel() + " at "
//						+ x + "," + y);
				// place linked nodes

				int countLinked = 0;
				// double angle = 0.0;

				for (LWComponent linkedNode : clusterMap.get(node)) {
					// LWNode nodeLinked = (LWNode)c;
					double angle = Math.PI * 2 * Math.random();
					
					double radiusX = radius
							* (1 - Math.pow(Math.random(), 2.0)) + nodeWidth;
					double radiusY =radius
							* (1 - Math.pow(Math.random(), 2.0)) + nodeHeight;
					double xLinkedNode = x + radiusX * Math.cos(angle);
					double yLinkedNode = y + radiusY * Math.sin(angle);

					boolean flag = true;
					int col_count = 0;
					while (flag && col_count < MAX_COLLISION_CHECK) {
						if ((VUE.getActiveViewer().pickNode((float) x,
								(float) y) != null)
								|| (VUE.getActiveViewer().pickNode(
										(float) x + node.getWidth(),
										(float) y + node.getHeight()) != null)
								|| (VUE.getActiveViewer().pickNode((float) x,
										(float) y + node.getHeight()) != null)
								|| (VUE.getActiveViewer().pickNode(
										(float) x + node.getWidth(), (float) y) != null)) {
							angle = Math.PI * 2 * Math.random();
							radiusX =  radius
									* (1 - Math.pow(Math.random(), 2.0))
									+ nodeWidth;
							radiusY =  radius
									* (1 - Math.pow(Math.random(), 2.0))
									+ nodeHeight;
							xLinkedNode = x + radiusX * Math.cos(angle);
							yLinkedNode = y + radiusY * Math.sin(angle);
							col_count++;
						} else {
							flag = false;
						}
					}
					linkedNode.setLocation(xLinkedNode, yLinkedNode);
					countLinked++;

				}
			}
		}
	}
}
