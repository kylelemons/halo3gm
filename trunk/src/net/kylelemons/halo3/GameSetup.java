/**
 * @file GameSetup.java by [AUTHOR], created Jan 21, 2009
 */
package net.kylelemons.halo3;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author eko
 * 
 */
public class GameSetup implements Serializable
{
  private static final long serialVersionUID = 8786786673968199764L;

  public static interface SetupChangeListener
  {
    public void setupChanged();
  }

  public static final int                          MAX_ALLOWED_TEAMS = 8;

  private int                                      m_teamcount;
  private int                                      m_fairness;
  private String[]                                 m_teamnames;
  private int[]                                    m_teamcaps;
  private String                                   m_serverhost;

  private transient ArrayList<SetupChangeListener> m_listeners;

  private int                                      m_gamedelay;

  private int                                      m_fair_teams;

  public GameSetup()
  {
    m_teamcount = 4;
    m_fairness = 1;
    m_gamedelay = 60 * 7;
    m_serverhost = "127.0.0.1";
    m_listeners = new ArrayList<SetupChangeListener>();
    m_teamnames = new String[MAX_ALLOWED_TEAMS];
    for (int i = 0; i < MAX_ALLOWED_TEAMS; ++i)
      m_teamnames[i] = TeamGrid.TeamNames[i];
    m_teamcaps = new int[MAX_ALLOWED_TEAMS];
    for (int i = 0; i < MAX_ALLOWED_TEAMS; ++i)
      m_teamcaps[i] = 0;
  }

  public void setTeamName(int team, String name)
  {
    if (team >= MAX_ALLOWED_TEAMS || team < 0) return;
    m_teamnames[team] = name;
    this.fireSetupChange();
  }

  public String getTeamName(int team)
  {
    if (team >= MAX_ALLOWED_TEAMS || team < 0) return null;
    return m_teamnames[team];
  }

  /**
   * @param teamcount
   *          the teamcount to set
   */
  public void setTeamCount(int teamcount)
  {
    m_teamcount = teamcount;
    fireSetupChange();
  }

  /**
   * @return the teamcount
   */
  public int getTeamCount()
  {
    return m_teamcount;
  }

  /**
   * @param fairness
   *          the fairness to set
   */
  public void setFairness(int fairness)
  {
    this.m_fairness = fairness;
    fireSetupChange();
  }

  /**
   * @return the fairness
   */
  public int getFairness()
  {
    return m_fairness;
  }

  public void addSetupChangeListener(SetupChangeListener c)
  {
    m_listeners.add(c);
  }

  public void removeSetupChangeListener(SetupChangeListener c)
  {
    m_listeners.remove(c);
  }

  private void fireSetupChange()
  {
    for (int i = 0; i < m_listeners.size(); ++i)
      m_listeners.get(i).setupChanged();
  }

  public void setTeamCap(int team, int maxPlayers)
  {
    if (team >= MAX_ALLOWED_TEAMS || team < 0) return;
    m_teamcaps[team] = maxPlayers;
    this.fireSetupChange();
  }

  public int getTeamCap(int team)
  {
    if (team >= MAX_ALLOWED_TEAMS || team < 0) return -1;
    return m_teamcaps[team];
  }

  /**
   * This does not fire the setup changed, as it shouldn't really affect the generated game
   * 
   * @param seconds
   *          How many seconds to wait in between games
   */
  public void setGameDelay(int seconds)
  {
    m_gamedelay = seconds;
  }

  public int getGameDelay()
  {
    return m_gamedelay;
  }

  public void setFairTeamCount(int i)
  {
    m_fair_teams = i;
    fireSetupChange();
  }

  public int getFairTeamCount()
  {
    // TODO Auto-generated method stub
    return m_fair_teams;
  }

  public String getServerHost()
  {
    return m_serverhost;
  }

  public void setServerHost(String serverHost)
  {
    this.m_serverhost = serverHost;
  }

}
