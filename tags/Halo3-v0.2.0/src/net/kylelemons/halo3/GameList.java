/**
 * @file GameList.java by [AUTHOR], created Jan 20, 2009
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
public class GameList extends AbstractListModel
{
  private static final long serialVersionUID = 7129627745941514380L;

  public static class GameType implements Serializable
  {
    private static final long serialVersionUID = 3091061241719209581L;
    public String name;
    public int weight;
    public boolean enabled;
    
    public GameType(String gameName, int gameWeight, boolean gameEnabled)
    {
      name = gameName;
      weight = gameWeight;
      enabled = gameEnabled;
    }
    
    public GameType(String gameName, int gameWeight)
    {
      this(gameName, gameWeight, true);
    }
    
    public GameType(String gameName)
    {
      this(gameName, 1, true);
    }
    
    public static class NameSorter implements Comparator<GameType>
    {
      public int compare(GameType o1, GameType o2)
      {
        return o1.name.compareTo(o2.name);
      }
    }
    
    public String toString()
    {
      return name + (enabled?"":" (disabled)");
    }
  }

  private ArrayList<GameType> m_gamelist;
  private boolean m_sorted;
  
  public GameList()
  {
    m_gamelist = new ArrayList<GameType>();
    m_sorted = true;
  }
  
  /** DO NOT modify the returned arraylist */
  public ArrayList<GameType> getGameList(boolean autosort)
  {
    if (!m_sorted && autosort) sort();
    return m_gamelist;
  }
  
  public ArrayList<GameType> getGameList()
  {
    return getGameList(true);
  }
  
  public void add(GameType g)
  {
    add(g, true);
  }
  
  public void add(GameType g, boolean autosort)
  {
    m_gamelist.add(g);
    m_sorted = false;
    if (autosort)
      sort();
  }
  
  public void updateGame(int index, GameType g, boolean autosort)
  {
    m_gamelist.set(index, g);
    m_sorted = false;
    if (autosort)
      sort();
  }
  
  public void updateGame(int index, GameType g)
  {
    updateGame(index, g, true);
  }
  
  public void sort()
  {
    Collections.sort(m_gamelist, new GameType.NameSorter());
    m_sorted = true;
    this.fireContentsChanged(this, 0, m_gamelist.size()-1);
  }
  
  /**
   * @see javax.swing.ListModel#getElementAt(int)
   */
  public Object getElementAt(int index)
  {
    if (!m_sorted) sort();
    return m_gamelist.get(index);
  }

  /**
   * @see javax.swing.ListModel#getSize()
   */
  public int getSize()
  {
    return m_gamelist.size();
  }

  public void clear()
  {
    int oldsize = m_gamelist.size();
    m_gamelist.clear();
    this.fireIntervalRemoved(this, 0, oldsize);
  }

}
