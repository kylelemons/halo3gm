/**
 * @file Team.java by [AUTHOR], created Jan 15, 2009
 */
package net.kylelemons.halo3;

import java.util.ArrayList;

/**
 * @author eko
 *
 */
public class Team
{
  private ArrayList<PlayerList.Player> m_members;
  private String m_teamname;
  
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
    for (int i = 0; i < m_members.size(); ++i)
      strings.add(m_members.get(i).getLongName());
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
    return skill;
  }
}
