/**
 * @file Team.java by [AUTHOR], created Jan 15, 2009
 */
package net.kylelemons.halo3;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author eko
 * 
 */
public class Team implements Serializable
{
  private static final long            serialVersionUID      = 2865566082926115926L;

  private static final double          TEAM_SKILL_PER_PLAYER = 1.0;

  private ArrayList<PlayerList.Player> m_members;
  private String                       m_teamname;

  public Team(String teamname)
  {
    m_teamname = teamname;
    m_members = new ArrayList<PlayerList.Player>();
  }

  public void clear()
  {
    m_members = new ArrayList<PlayerList.Player>();
  }

  public void add(PlayerList.Player p)
  {
    m_members.add(p);
  }

  public int size()
  {
    return m_members.size();
  }

  public String getTeamName()
  {
    return m_teamname;
  }

  public ArrayList<String> getMembers()
  {
    ArrayList<String> strings = new ArrayList<String>();
    for (int i = 0; i < m_members.size(); ++i)
      strings.add(m_members.get(i).toString());
    /*
     * for (int i = 0; i < m_members.size(); ++i) strings.add(m_members.get(i).getLongName());
     */
    return strings;
  }

  public void setTeamName(String teamName)
  {
    m_teamname = teamName;
  }

  public int totalSkill()
  {
    int skill = 0;
    for (int i = 0; i < m_members.size(); ++i)
      skill += m_members.get(i).skill;
    skill += Math.ceil(m_members.size() * TEAM_SKILL_PER_PLAYER);
    return skill;
  }
}
