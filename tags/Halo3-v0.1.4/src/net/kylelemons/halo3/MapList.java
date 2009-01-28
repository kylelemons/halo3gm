/**
 * @file MapList.java by [AUTHOR], created Jan 21, 2009
 */
package net.kylelemons.halo3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractListModel;

/**
 * @author eko
 *
 */
public class MapList extends AbstractListModel
{
  private static final long serialVersionUID = 2788464546156207074L;

  public static class Map implements Serializable
  {
    private static final long serialVersionUID = 974076997077363698L;
    String name;
    int min_players;
    int max_players;
    int weight;
    boolean enabled;
    
    String image;
    
    public Map(String n, String imageURL, int min, int max, int w, boolean e)
    {
      name = n;
      min_players = min;
      max_players = max;
      weight = w;
      enabled = e;
      image = imageURL;
    }
    
    public Map(String n, String imageURL, int min, int max, int w)
    {
      this(n,imageURL,min,max,w, true);
    }
    
    public Map(String n, String imageURL, int min, int max)
    {
      this(n,imageURL,min,max,1, true);
    }
    
    public Map(String n, String imageURL)
    {
      this(n,imageURL,2,8,1, true);
    }
    
    public Map(String n)
    {
      this(n, "", 2, 8, 1, true);
      System.err.println("WARNING: Created MAP with empty image!");
    }
    
    public static class NameSorter implements Comparator<Map>
    {
      public int compare(Map o1, Map o2)
      {
        return o1.name.compareTo(o2.name);
      }
    }
    
    public String toString()
    {
      return name + (enabled?"":" (disabled)");
    }
  }
  
  ArrayList<Map> m_maplist;
  boolean m_sorted;
  
  public MapList()
  {
    m_maplist = new ArrayList<Map>();
    m_sorted = true;
  }
  
  /**
   * DO NOT MODIFY THIS.
   * @return A reference to the map list.
   */
  public ArrayList<Map> getMapList(boolean autosort)
  {
    if (!m_sorted && autosort) sort();
    return m_maplist;
  }
  
  public ArrayList<Map> getMapList()
  {
    return getMapList(true);
  }
  
  /**
   * Add a new map
   * @param m The map to add
   * @param autosort True to automatically sort
   */
  public void add(Map m, boolean autosort)
  {
    m_maplist.add(m);
    m_sorted = false;
    if (autosort)
      sort();
  }
  
  public void add(Map m)
  {
    add(m, true);
  }
  
  public void sort()
  {
    if (m_sorted) return;
    Collections.sort(m_maplist, new Map.NameSorter());
    m_sorted = true;
    this.fireContentsChanged(this, 0, m_maplist.size()-1);
  }

  /**
   * @see javax.swing.ListModel#getElementAt(int)
   */
  public Object getElementAt(int index)
  {
    if (!m_sorted) sort();
    return m_maplist.get(index);
  }

  /**
   * @see javax.swing.ListModel#getSize()
   */
  public int getSize()
  {
    return m_maplist.size();
  }

  public void updateMap(int idx, Map updated, boolean autosort)
  {
    m_maplist.set(idx, updated);
    m_sorted = false;
    if (autosort)
      sort();
  }
  
  public void updateMap(int idx, Map updated)
  {
    updateMap(idx, updated, true);
  }

  public void clear()
  {
    int oldsize = m_maplist.size();
    m_maplist.clear();
    this.fireIntervalRemoved(this, 0, oldsize);
  }

}
