/**
 * @file PlayerList.java by Kyle Lemons, created Jan 17, 2009
 */
package net.kylelemons.halo3;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractListModel;

/**
 * @author eko
 *
 */
public class PlayerList extends AbstractListModel
{
  private static final long serialVersionUID = 2562961543038299451L;

  public static class Player implements Serializable
  {
    private static final long serialVersionUID = 8275059106471746369L;
    public String name;
    public String gamertag;
    public String passcode_hash;
    public boolean playing;
    public boolean veto_power;
    
    /** 0..10 */
    public int skill;
    
    /** 0..100 */
    public int priority;
    
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_TAG = 2;
    public static final int SORT_BY_SKILL = 3;
    public static final int SORT_BY_PRIORITY = 4;

    public static class NameSorter implements Comparator<Player>
    {
      public int compare(Player o1, Player o2)
      {
        return o1.name.compareTo(o2.name);
      }
    }
    public static class GamerTagSorter implements Comparator<Player>
    {
      public int compare(Player o1, Player o2)
      {
        return o1.gamertag.compareTo(o2.gamertag);
      }
    }
    public static class SkillSorter implements Comparator<Player>
    {
      public int compare(Player o1, Player o2)
      {
        if (o1.playing && !o2.playing)
          return -1;
        if (!o1.playing && o2.playing)
          return 1;
        return o2.skill - o1.skill;
      }
    }
    public static class PrioritySorter implements Comparator<Player>
    {
      public int compare(Player o1, Player o2)
      {
        if (o1.playing && !o2.playing)
          return -1;
        if (!o1.playing && o2.playing)
          return 1;
        return o1.priority - o2.priority;
      }
    }
    
    public Player()
    {
      this("", "", 0, 0, false);
    }
    
    public Player(String n, String t)
    {
      this(n, t, 1, 50, true);
    }
    
    public Player(String n, String t, int skl)
    {
      this(n, t, skl, 50, true);
    }
    
    public Player(String n, String t, int skl, int prio, boolean p)
    {
      name = n;
      gamertag = t;
      playing = p;
      skill = skl;
      priority = prio;
      passcode_hash = "";
      veto_power = false;
    }
    
    public void setPasscode(String code)
    {
      try
      {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        passcode_hash = new String(md.digest(code.getBytes()));
      }
      catch (NoSuchAlgorithmException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.exit(1);
      }
    }
    
    public String getLongName()
    {
      return name + " (" + gamertag + ")" + (playing?"":" (Not Playing)");
    }
    
    public String toString()
    {
      return name;
    }
  }
  
  private ArrayList<Player> m_players;
  private boolean m_sorted;
  private int m_sorttype;
  
  public PlayerList()
  {
    m_sorted = true;
    m_sorttype = 0;
    m_players = new ArrayList<Player>();
  }
  
  /** DO NOT modify the returned arraylist 
   * @param autosort */
  public ArrayList<Player> getPlayers(boolean autosort)
  {
    if (!m_sorted && autosort) sort(m_sorttype);
    return m_players;
  }
  
  public ArrayList<Player> getPlayers()
  {
    return getPlayers(true);
  }
  
  public void add(Player p)
  {
    m_sorted = false;
    m_players.add(p);
    this.fireContentsChanged(this, 0, m_players.size()-1);
  }
  
  public void sort(int how)
  {
    switch (how)
    {
      case Player.SORT_BY_PRIORITY:
        Collections.sort(m_players, new Player.PrioritySorter());
        break;
      case Player.SORT_BY_SKILL:
        Collections.sort(m_players, new Player.SkillSorter());
        break;
      case Player.SORT_BY_TAG:
        Collections.sort(m_players, new Player.GamerTagSorter());
        break;
      case Player.SORT_BY_NAME:
      default:
        Collections.sort(m_players, new Player.NameSorter());
    }
    m_sorted = true;
    this.fireContentsChanged(this, 0, m_players.size()-1);
  }
  
  public int findPlayer(Player p)
  {
    return m_players.indexOf(p);
  }
  
  public void updatePlayer(int index, Player p)
  {
    m_players.set(index, p);
    m_sorted = false;
    this.fireContentsChanged(this, index, index);
  }

  public Object getElementAt(int index)
  {
    if (!m_sorted) sort(m_sorttype);
    return m_players.get(index).getLongName();
  }

  public int getSize()
  {
    return m_players.size();
  }

  public void delete(Player p)
  {
    while (m_players.remove(p));
    this.fireContentsChanged(this, 0, m_players.size()-1);
  }

  public void clear()
  {
    int oldsize = m_players.size();
    m_players.clear();
    this.fireIntervalRemoved(this, 0, oldsize);
  }
  
}
