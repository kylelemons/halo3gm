/**
 * @file GameGenerator.java by Kyle Lemons, created Jan 17, 2009
 */
package net.kylelemons.halo3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.logging.Logger;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * @author eko
 *
 */
public class GameGenerator implements ListDataListener
{
  public static interface GameChangedListener
  {
    public void gameChanged(GameGenerator gen);
  }
  
  private static Logger logger = Logger.getLogger("net.kylelemons.halo3");
  
  /** Keep track of the player list */
  private PlayerList m_playerlist;
  
  /** Keep track of the game list */
  private GameList m_gamelist;
  
  /** Keep track of the map list */
  private MapList m_maplist;
  
  /** Keep track of listeners */
  private ArrayList<GameChangedListener> m_listeners;
  
  /** Keep track of the setup */
  private GameSetup m_setup;
  
  /** Save the generated teams */
  private Team[] m_teams;
  
  /** Save the generated map */
  private MapList.Map m_map;
  
  /** Save the generated game */
  private GameList.GameType m_game;

  private long m_seed;
  private Random m_rand;
  private boolean m_good;
  
  public GameGenerator(GameSetup setup)
  {
    m_playerlist = new PlayerList();
    m_gamelist = new GameList();
    m_maplist = new MapList();
    m_setup = setup;
    m_listeners = new ArrayList<GameChangedListener>();
    m_rand = new Random();
    m_seed = -1;
    m_good = true;
  }
  
  public GameGenerator()
  {
    this(new GameSetup());
  }
  
  public void setPlayerList(PlayerList list)
  {
    m_playerlist = list;
    m_playerlist.addListDataListener(this);
  }
  
  public void setGameList(GameList list)
  {
    m_gamelist = list;
    m_gamelist.addListDataListener(this);
  }
  
  public void setMapList(MapList list)
  {
    m_maplist = list;
    m_maplist.addListDataListener(this);
  }

  public void contentsChanged(ListDataEvent e)
  {
    regenerate();
  }

  public void intervalAdded(ListDataEvent e)
  {
    regenerate();
  }

  public void intervalRemoved(ListDataEvent e)
  {
    regenerate();
  }
  
  public synchronized void regenerate()
  {
    // Don't do anything until the game has been explicitly generated
    if (m_seed == -1)
      return;
    
    // Regenerate the game
    if (m_seed == 0)
    {
      m_seed = System.currentTimeMillis();
      logger.info("Generating game...");
    }
    else
      logger.info("Regenerating game...");
    
    // Reset the PRNG
    m_rand.setSeed(m_seed);
    
    // Wipe the team list clean
    m_teams = new Team[m_setup.getTeamCount()];
    for (int t = 0; t < m_teams.length; ++t)
    {
      m_teams[t] = new Team("Team " + (t+1));
    }
    
    // Collect all of the players
    ArrayList<PlayerList.Player> players = new ArrayList<PlayerList.Player>();
    for (int i = 0; i < m_playerlist.getSize(); ++i)
    {
      PlayerList.Player p = m_playerlist.getPlayers(false).get(i); // don't autosort! This calls this method again!
      if (p.playing)
        players.add(p);
    }
    
    // Order list by: Priority, Skill, Random
    Collections.shuffle(players, m_rand);
    Collections.sort(players, new PlayerList.Player.SkillSorter());    // guaranteed to be stable, so it preserves
    Collections.sort(players, new PlayerList.Player.PrioritySorter()); // prior order when equal
    
    int totalPlayers = players.size();
    if (totalPlayers < 2)
    {
      logger.info("Cowardly refusing to create a game with fewer than two people playing");
      return;
    }
    
    int realPlayers = (totalPlayers <= 16)?totalPlayers:16;
    int maxPerTeam = (int)Math.ceil( (double)totalPlayers / m_setup.getTeamCount() );
    logger.fine("With " + players.size() + " players, we'll have at most " + maxPerTeam + " players per team");
    
    int[] teamWeights = new int[m_teams.length];
    int fairness = m_setup.getFairness();
    
    for (int p = 0; p < players.size(); ++p)
    {
      // Assign each player to a team
      /* Here's how weights work:
       *   Each team that has available spaces starts out with a weight of 1
       *   If the fairness is positive, teams get extra weight (equal to fairness) for each empty space
       *   If the fairness is negative, teams get extra weight (equal to fairness) for each filled space
       *   If the fairness is zero, teams get no extra weight
       */
      for (int t = 0; t < m_teams.length; ++t)
      { 
        // This facilitates the rand() < x comparisons
        if (t > 0) teamWeights[t] = teamWeights[t-1];
        else       teamWeights[t] = 0;
        
        int max = maxPerTeam;
        if (m_setup.getTeamCap(t) > 0 && m_setup.getTeamCap(t) < maxPerTeam)
          max = m_setup.getTeamCap(t);
        
        if (m_teams[t].size() < max)
        {
          // Each team has an automatic weight of 1 if it has available spaces
          teamWeights[t] += 1;
        
          // Weights are increased by fairness as explained above
          if (fairness > 0)
            teamWeights[t] += fairness * (maxPerTeam - m_teams[t].size());
          if (fairness < 0)
            teamWeights[t] += -fairness * m_teams[t].size();
        }
      }
      
      if (teamWeights[m_teams.length-1] > 0)
      {
        // Choose a number within the bounds of these weights
        int chosen = m_rand.nextInt(teamWeights[m_teams.length-1]);
        
        // Figure out what team that is
        int team = m_teams.length; // crash if not found
        for (int t = 0; t < m_teams.length; ++t)
          if (chosen < teamWeights[t])
          {
            team = t;
            break;
          }
        
        m_teams[team].add(players.get(p));
        m_good = true;
      }
      else
      {
        m_good = false;
        logger.warning("Team caps are preventing full allocation of players!");
        break;
      }
    }
    
    int[] mapWeights = new int[m_maplist.getSize()];
    for (int m = 0; m < mapWeights.length; ++m)
    {
      if (m == 0) mapWeights[m] = 0;
      else        mapWeights[m] = mapWeights[m-1];
      
      MapList.Map map = m_maplist.getMapList(false).get(m);
      
      if (map.enabled && map.min_players <= realPlayers && realPlayers <= map.max_players)
        mapWeights[m] += map.weight;
    }
    
    if (m_maplist.getSize() < 1 || mapWeights[mapWeights.length-1] <= 0)
    {
      logger.info("Cowardly refusing to create a game with fewer than one valid map");
      return;
    }    
    
    int chosenMap = m_rand.nextInt(mapWeights[mapWeights.length-1]);
    for (int m = 0; m < mapWeights.length; ++m)
      if (chosenMap < mapWeights[m])
      {
        m_map = m_maplist.getMapList(false).get(m);
        break;
      }

    int[] gameWeights = new int[m_gamelist.getSize()];
    for (int g = 0; g < gameWeights.length; ++g)
    {
      if (g == 0) gameWeights[g] = 0;
      else        gameWeights[g] = gameWeights[g-1];
      
      GameList.GameType game = m_gamelist.getGameList(false).get(g);
      
      if (game.enabled)
        gameWeights[g] += game.weight;
    }
    
    if (m_gamelist.getSize() < 1 || gameWeights[gameWeights.length-1] <= 0)
    {
      logger.info("Cowardly refusing to create a game with fewer than one valid game");
      return;
    }
    
    int chosenGame = m_rand.nextInt(gameWeights[gameWeights.length-1]);
    for (int g = 0; g < gameWeights.length; ++g)
      if (chosenGame < gameWeights[g])
      {
        m_game = m_gamelist.getGameList(false).get(g);
        break;
      }
    
    fireGameChanged();
  }
  
  public void generate()
  {
    m_seed = 0;
    regenerate();
  }
  
  public void addGameChangedListener(GameChangedListener listener)
  {
    m_listeners.add(listener);
  }
  
  private void fireGameChanged()
  {
    ArrayList<Integer> deleteIndices = new ArrayList<Integer>();
    for (int i = 0; i < m_listeners.size(); ++i)
    {
      try
      {
        m_listeners.get(i).gameChanged(this);
      }
      catch (Exception e)
      {
        e.printStackTrace();
        logger.severe("Exception in listener! Deleting.");
        deleteIndices.add(i);
      }
    }
    // Remove from last to first to maintain indices
    for (int d = deleteIndices.size()-1; d >= 0; ++d)
    {
      m_listeners.remove(deleteIndices.get(d));
    }
  }
  
  public Team[] getTeams()
  {
    return m_teams;
  }

  public GameList.GameType getGame()
  {
    return m_game;
  }

  public MapList.Map getMap()
  {
    return m_map;
  }

  public void setSetup(GameSetup setup)
  {
    m_setup = setup;
    regenerate();
  }

  public boolean getGood()
  {
    // TODO Auto-generated method stub
    return m_good;
  }
}
