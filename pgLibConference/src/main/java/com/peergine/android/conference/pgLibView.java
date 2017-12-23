/*************************************************************************
  copyright   : Copyright (C) 2014-2017, Peergine, All rights reserved.
              : www.peergine.com, www.pptun.com
              :
  filename    : pgLibView.java
  discription : 
  modify      : create, chenbichao, 2017/05/10

*************************************************************************/

package com.peergine.android.conference;


import android.view.SurfaceView;

import com.peergine.plugin.lib.pgLibJNINode;

import java.util.ArrayList;

/**
 * @author ctkj
 * View对象管理类
 */
public class pgLibView {
	
	static class NodeItem {
		public String sID = "";
		public pgLibJNINode Node = null;
		public SurfaceView View = null;
		public NodeItem(String sID, pgLibJNINode Node, SurfaceView View) {
			this.sID = sID;
			this.Node = Node;
			this.View = View;
		}
	}
	
	private static ArrayList<NodeItem> s_listItem = new ArrayList<NodeItem>();

	/**
	 * 通过ViewID生成View对象
	 * @param sViewID 字符串ViewID
	 * @return View对象
	 */
	public static SurfaceView Get(String sViewID) {
		try {
			SurfaceView View = null;

			synchronized(s_listItem) {
				for (int i = 0; i < s_listItem.size(); i++) {
					NodeItem item = s_listItem.get(i);
					if (sViewID.equals(item.sID)) {
						View = item.View;
						break;
					}
				}

				if (View == null) {
					try {
						pgLibJNINode Node = new pgLibJNINode();
						View = (SurfaceView)Node.WndNew(0, 0, 320, 240);
						if (View != null) {
							s_listItem.add(new NodeItem(sViewID, Node, View));
							System.out.println("pgLibView.Get: View add, sViewID=" + sViewID);
						}
						else {
							Node = null;
						}
					}
					catch (Exception ex) {
						ex.printStackTrace();
						View = null;
					}
				}
			}

			return View;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * 释放View
	 * @param View View对象
	 */
	public static void Release(SurfaceView View) {
		try {
			if (View != null && View.getParent() != null) {
				System.out.println("pgLibView.Release: The view has attached in the parent layout!");
				return;
			}

			synchronized(s_listItem) {
				for (int i = 0; i < s_listItem.size(); i++) {
					NodeItem item = s_listItem.get(i);
					if (View == item.View) {
						System.out.println("pgLibView.Release: View delete, sViewID=" + item.sID);
						s_listItem.remove(i);
						break;
					}
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 通过View获取Node
	 * @param View View对象
	 * @return Node对象
	 */
	public static pgLibJNINode GetNodeByView(SurfaceView View) {
		try {
			pgLibJNINode Node = null;
			synchronized(s_listItem) {
				for (int i = 0; i < s_listItem.size(); i++) {
					NodeItem item = s_listItem.get(i);
					if (View == item.View) {
						Node = item.Node;
						break;
					}
				}
			}
			return Node;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
