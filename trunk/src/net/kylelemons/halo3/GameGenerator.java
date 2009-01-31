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
 * This class encapsulates all of the logic necessary to generate the game, map, and players who will be playing on them for the Halo3 Game Master.
 * 
 * @author eko
 * 
 */
public class GameGenerator implements ListDataListener
{
  /**
   * All classes who want to be notified when the game changes should implement this. The listeners can be added and removed with the
   * {@link GameGenerator#addGameChangedListener(GameChangedListener)} and {@link GameGenerator#removeGameChangedListener(GameChangedListener)} utility methods
   * of GameGenerator.
   * 
   * @author eko
   */
  public static interface GameChangedListener
  {
    /**
     * This gets called when the generator is finished generating (or regenerating) the game.
     * 
     * @param gen
     *          The game generator calling the method (usually this)
     */
    public void gameChanged(GameGenerator gen);
  }

  private static Logger                  logger = Logger.getLogger("net.kylelemons.halo3");

  /** Keep track of the player list */
  private PlayerList                     m_playerlist;

  /** Keep track of the game list */
  private GameList                       m_gamelist;

  /** Keep track of the map list */
  private MapList                        m_maplist;

  /** Keep track of listeners */
  private ArrayList<GameChangedListener> m_listeners;

  /** Keep track of the setup */
  private GameSetup                      m_setup;

  /** Save the generated teams */
  private Team[]                         m_teams;

  /** Save the generated map */
  private MapList.Map                    m_map;

  /** Save the generated game */
  private GameList.GameType              m_game;

  /**
   * This seed is generate()d and used for regenerate()s to keep things consistent
   */
  private long                           m_seed;

  /**
   * This is used to perform all of the random number stuff. It should be consistent across Java installs
   */
  private Random                         m_rand;

  /**
   * Is the player allocation good? If this is false, somebody didn't get allocated right
   */
  private boolean                        m_good;

  /**
   * Create a new game generator with the parameters given in the setup.
   * 
   * @param setup
   *          The game setup to use for generating games
   */
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

  /**
   * Create a game generator with the default setup. Be warned-- you can't access this setup from elsewhere. This is NOT the preferred way to create one.
   * 
   * @deprecated in favor of {@link GameGenerator#GameGenerator(GameSetup)}
   */
  @Deprecated
  public GameGenerator()
  {
    this(new GameSetup());
  }

  /**
   * Set the player list
   * 
   * @param list
   *          The player list to set
   */
  public void setPlayerList(PlayerList list)
  {
    m_playerlist = list;
    m_playerlist.addListDataListener(this);
  }

  /**
   * Set the game list
   * 
   * @param list
   *          The game list to set
   */
  public void setGameList(GameList list)
  {
    m_gamelist = list;
    m_gamelist.addListDataListener(this);
  }

  /**
   * Set the map list from which the map will be randomly selected
   * 
   * @param list
   *          The map list
   */
  public void setMapList(MapList list)
  {
    m_maplist = list;
    m_maplist.addListDataListener(this);
  }

  /**
   * @see javax.swing.event.ListDataListener#contentsChanged(javax.swing.event.ListDataEvent)
   */
  public void contentsChanged(ListDataEvent e)
  {
    regenerate();
  }

  /**
   * @see javax.swing.event.ListDataListener#intervalAdded(javax.swing.event.ListDataEvent)
   */
  public void intervalAdded(ListDataEvent e)
  {
    regenerate();
  }

  /**
   * @see javax.swing.event.ListDataListener#intervalRemoved(javax.swing.event.ListDataEvent)
   */
  public void intervalRemoved(ListDataEvent e)
  {
    regenerate();
  }

  /**
   * Regenerate the game with the same seed
   */
  public synchronized void regenerate()
  {
    // Don't do anything until the game has been explicitly generated
    if (m_seed == -1) return;

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
    m_teams = null;

    // Collect all of the players
    ArrayList<PlayerList.Player> players = new ArrayList<PlayerList.Player>();
    for (int i = 0; i < m_playerlist.getSize(); ++i)
    {
      // We specifically keep this from autosorting, because that would cause
      // this method to be called in a loop...
      PlayerList.Player p = m_playerlist.getPlayers(false).get(i);
      if (p.playing) players.add(p);
    }

    /* * * * * * * * * * * * * * * * */
    /* Start Player allocation code */
    /* * * * * * * * * * * * * * * * */
    int totalPlayers = players.size();
    int realPlayers = (totalPlayers <= 16) ? totalPlayers : 16;
    int maxPerTeam = (int) Math.ceil((double) totalPlayers / m_setup.getTeamCount());
    int fairness = m_setup.getFairness();
    if (totalPlayers < 2)
    {
      logger.info("Cowardly refusing to create a game with fewer than two people playing");
      return;
    }

    logger.fine("With " + players.size() + " players, we'll have at most " + maxPerTeam + " players per team");

    // Loop fairness times to find the fairest team allocation
    int fairest_score = -1;
    for (int i = 0; i <= fairness; ++i)
    {
      int fairness_score;

      // Generate the teams
      Collections.shuffle(players, m_rand);

      // Allocate a clean list of teams
      Team[] potential = new Team[m_setup.getTeamCount()];

      // Assign people to teams
      int cur = 0;
      for (int t = 0; t < potential.length; ++t)
      {
        int cap = m_setup.getTeamCap(t);
        if (cap == 0 || cap > maxPerTeam) cap = maxPerTeam;
        potential[t] = new Team("Team " + (t + 1));
        for (int j = 0; j < cap && cur < players.size(); ++j)
          potential[t].add(players.get(cur++));
      }
      // This is good if the teams come out evenly. Otherwise, the caps are
      // messed up.
      m_good = cur == players.size();

      // Here comes the very basic "genetic algorithm" code.
      fairness_score = getFairTeamScore(potential);

      // Keep the fairest matchup
      if (m_teams == null || fairness_score < fairest_score)
      {
        m_teams = potential;
        fairest_score = fairness_score;
        logger.info("New fairest team: " + fairest_score);
      }
    }

    // *** Explanation of weights (for both maps and games):
    // We create an array of the boundaries of each map or game, such that a
    // random choice within the full range has the applicable probabilities.
    // Note that each boundary is [) (e.g. lower inclusive, upper exclusive).
    // For example:
    // An array of [3,3,4,10] is equivalent to saying
    // [0 .. 3][3 .. 3][3 .. 4][5 .. 10]
    // ^^^^^^^^ Map 1 has a weight of 3, so if the random generator picks 0,1,or 2
    // this map gets chosen.
    // ________^^^^^^^^ This game has a weight of zero, so no number will cause it
    // to be selected by the generator
    // ________________^^^^^^^^ This has a weight of one. Only a 3 will select it.
    // ________________________^^^^^^^^^ This has a weight of 5. It'll probably be chosen.

    // See description above for how these weights work.
    int[] mapWeights = new int[m_maplist.getSize()];
    for (int m = 0; m < mapWeights.length; ++m)
    {
      if (m == 0)
        mapWeights[m] = 0;
      else
        mapWeights[m] = mapWeights[m - 1];

      MapList.Map map = m_maplist.getMapList(false).get(m);

      if (map.enabled && map.min_players <= realPlayers && realPlayers <= map.max_players) mapWeights[m] += map.weight;
    }

    // See description above for how these weights work.
    if (m_maplist.getSize() < 1 || mapWeights[mapWeights.length - 1] <= 0)
    {
      logger.info("Cowardly refusing to create a game with fewer than one valid map");
      return;
    }

    // See description above for how these weights work.
    int chosenMap = m_rand.nextInt(mapWeights[mapWeights.length - 1]);
    for (int m = 0; m < mapWeights.length; ++m)
      if (chosenMap < mapWeights[m])
      {
        m_map = m_maplist.getMapList(false).get(m);
        break;
      }

    // See description above for how these weights work.
    int[] gameWeights = new int[m_gamelist.getSize()];
    for (int g = 0; g < gameWeights.length; ++g)
    {
      if (g == 0)
        gameWeights[g] = 0;
      else
        gameWeights[g] = gameWeights[g - 1];

      GameList.GameType game = m_gamelist.getGameList(false).get(g);

      if (game.enabled) gameWeights[g] += game.weight;
    }

    // Sanity check
    if (m_gamelist.getSize() < 1 || gameWeights[gameWeights.length - 1] <= 0)
    {
      logger.info("Cowardly refusing to create a game with fewer than one valid game");
      return;
    }

    // See description above for how these weights work.
    int chosenGame = m_rand.nextInt(gameWeights[gameWeights.length - 1]);
    for (int g = 0; g < gameWeights.length; ++g)
      if (chosenGame < gameWeights[g])
      {
        m_game = m_gamelist.getGameList(false).get(g);
        break;
      }

    // Let everyone know we updated the game
    fireGameChanged();
  }

  /**
   * This calculates the fitness of the proposed solution to the "fair game" problem. The lower this number is, the more fair the matchup.
   * 
   * @param potential
   *          The team matchup to examine
   * @return A number indicating how unfair the game is.
   */
  private int getFairTeamScore(Team[] potential)
  {
    int max;
    int min;

    if (potential.length <= 0) return -1;

    max = min = potential[0].totalSkill();

    for (int t = 1; t < potential.length; ++t)
    {
      if (t == potential.length - 1 && m_setup.getIgnoreLastTeam()) continue;
      int skill = potential[t].totalSkill();
      if (skill > max) max = skill;
      if (skill < min) min = skill;
    }

    return max - min;
  }

  /**
   * Create a new seed and generate the game afresh
   */
  public void generate()
  {
    m_seed = 0;
    regenerate();
  }

  /**
   * Add a listener to be notified of changes
   * 
   * @param listener
   *          The object to notify
   */
  public void addGameChangedListener(GameChangedListener listener)
  {
    m_listeners.add(listener);
  }

  /**
   * Remove instead of add. {@link #addGameChangedListener(GameChangedListener)}
   * 
   * @param listener
   */
  public void removeGameChanedListener(GameChangedListener listener)
  {
    m_listeners.remove(listener);
  }

  /**
   * Notify all listening objects that the game has changed. I really don't know if the exception catching here does what it should. Anyone? TODO
   */
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
    for (int d = deleteIndices.size() - 1; d >= 0; ++d)
    {
      m_listeners.remove(deleteIndices.get(d));
    }
  }

  /**
   * Get the list of teams that was generated
   * 
   * @return An array of teams
   */
  public Team[] getTeams()
  {
    return m_teams;
  }

  /**
   * Get the game type that was chosen
   * 
   * @return The chosen game
   */
  public GameList.GameType getGame()
  {
    return m_game;
  }

  /**
   * Get the map that was chosen
   * 
   * @return The chosen map
   */
  public MapList.Map getMap()
  {
    return m_map;
  }

  /**
   * Set the game generator to use the given game setup for generating games. This should normally be set in the constructor.
   * 
   * Causes a regeneration.
   * 
   * @param setup
   *          The new setup to use
   */
  public void setSetup(GameSetup setup)
  {
    m_setup = setup;
    regenerate();
  }

  /**
   * Answer the question: "Did all of the players get allocated properly?"
   * 
   * @return The answer.
   */
  public boolean getGood()
  {
    // TODO Auto-generated method stub
    return m_good;
  }
}
