package net.kylelemons.halo3;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

public class UserInterface implements KeyListener, ChangeListener, ActionListener, DocumentListener, ListSelectionListener, ItemListener, PropertyChangeListener
{
  private static final int FRAME_MIN_SIZE = 300;
  
  public static final String DEFAULT_DATABASE = "Halo3.dat";
  
  private static Logger logger = Logger.getLogger("net.kylelemons.halo3");
  
  private JFrame m_fsframe;
  private JLabel m_maplabel;
  private JPanel m_maincontent;
  private TeamGrid m_teamgrid;
  private JSplitPane m_playereditor;
  private PlayerList m_playerlist;
  private JList m_playerlistview;
  private PlayerList.Player m_active_player;
  private JTextField m_active_playername;
  private JTextField m_active_gamertag;
  private JSlider m_active_skill;
  private JCheckBox m_active_enabled;
  private JCheckBox m_vetopower;
  private JPasswordField m_vetopasscode;
  private JSplitPane m_gameeditor;
  private GameList m_gamelist;
  private JList m_gamelistview;
  private JScrollPane m_gamesettingspane;
  private MapList m_maplist;
  private JList m_maplistview;
  private JScrollPane m_mapsettingspane;
  private ArrayList<KeyListener> m_fskeylisteners;
  private JScrollPane m_setupeditor;
  private GameSetup m_setup;
  private JCheckBox m_delete_confirm;
  private JProgressBar m_gametime;
  private SwingWorker m_timerunner;
  private long m_gamestarttime;
  private JTextField m_databasename;
  private JLabel m_databasestatus;
  private JComponent m_pendingscreen;

  private JButton m_loaddbbutton;

  private JPanel m_authenticate;

  private JPasswordField m_authpassfield;
  
  // TODO: Pending screen login
  // TODO: Setup parameter for "ignore last team"
  
  public static interface KeyListener
  {
    public void MainScreenKeyHit(int keyCode, int eventID); // KeyEvent.VK_* key codes
  }

  /** Create a new User Interface for the Halo 3 Game Master */
  public UserInterface()
  {
    m_active_player = null;
    createWindow();
    makeFullScreen();
    m_fskeylisteners = new ArrayList<KeyListener>();
    m_timerunner = null;
    m_databasestatus = null;
  }
  
  public void setup()
  {
    loadDatabase(DEFAULT_DATABASE);
    createContents();
    showWindow();
  }
  
  private void loadDatabase(String filename)
  {
    if (m_databasestatus == null) // not setup() yet
      m_databasestatus = new JLabel(); // just make a dummy

    if (filename.length() <= 0)
    {
      m_databasestatus.setText("Empty filenames are not acceptable.");
      return;
    }
    
    try
    {
      InputStream file = new FileInputStream( filename );
      InputStream buffer = new BufferedInputStream( file );
      ObjectInput input = new ObjectInputStream( buffer );
      try
      {
        Integer playerCount = (Integer)input.readObject();
        m_playerlist.clear();
        
        logger.info("Loading " + playerCount + " players...");
        m_databasestatus.setText("Loading " + playerCount + " players...");
        for (int i = 0; i < playerCount; ++i)
        {
          PlayerList.Player next = (PlayerList.Player)input.readObject();
          //next.passcode_hash = "";
          //next.veto_power = false;
          m_playerlist.add(next);
        }

        Integer mapCount = (Integer)input.readObject();
        m_maplist.clear();
        
        logger.info("Loading " + mapCount + " maps...");
        m_databasestatus.setText("Loading " + mapCount + " maps...");
        for (int i = 0; i < mapCount; ++i)
        {
          MapList.Map next = (MapList.Map)input.readObject();
          m_maplist.add(next);
        }

        Integer gameTypeCount = (Integer)input.readObject();
        m_gamelist.clear();
        
        logger.info("Loading " + gameTypeCount + " games...");
        m_databasestatus.setText("Loading " + gameTypeCount + " games...");
        for (int i = 0; i < gameTypeCount; ++i)
        {
          GameList.GameType next = (GameList.GameType)input.readObject();
          m_gamelist.add(next);
        }
        
        logger.info("Loading Game Setup");
        m_databasestatus.setText("Loading Game Setup...");
        m_setup.setFairness((Integer)input.readObject());
        m_setup.setGameDelay((Integer)input.readObject());
        m_setup.setTeamCount((Integer)input.readObject());
        m_setup.setIgnoreLast((Boolean)input.readObject());
        for (int i = 0; i < GameSetup.MAX_ALLOWED_TEAMS; ++i)
        {
          m_setup.setTeamCap(i, (Integer)input.readObject());
          m_setup.setTeamName(i, (String)input.readObject());
        }
        
        logger.info("Database loaded successfully");
        m_databasestatus.setText("Database loaded successfully");
      }
      catch(ClassCastException ex)
      {
        logger.log(Level.SEVERE, "Attempted to read the wrong class!", ex);
      }
      finally
      {
        input.close();
      }
    }
    catch(FileNotFoundException ex) {
      m_databasestatus.setText("Could not load file: " + filename);
    }
    catch(ClassNotFoundException ex) {
      logger.log(Level.SEVERE, "Cannot perform input. Class not found.", ex);
    }
    catch(IOException ex) {
      logger.log(Level.SEVERE, "Cannot perform input.", ex);
    }
  }
  
  private void saveDatabase(String filename)
  {
    if (m_databasestatus == null) // not setup() yet
      m_databasestatus = new JLabel(); // just make a dummy

    if (filename.length() <= 0)
    {
      m_databasestatus.setText("Empty filenames are not acceptable.");
      return;
    }
    
    try
    {
      // use buffering
      OutputStream file = new FileOutputStream(filename);
      OutputStream buffer = new BufferedOutputStream(file);
      ObjectOutput output = new ObjectOutputStream(buffer);
      try
      {
        Integer playerCount = new Integer(m_playerlist.getSize());
        output.writeObject(playerCount);

        logger.info("Writing " + playerCount + " players...");
        m_databasestatus.setText("Saving " + playerCount + " players...");
        for (int i = 0; i < playerCount; ++i)
          output.writeObject(m_playerlist.getPlayers().get(i));
        
        Integer mapCount = new Integer(m_maplist.getSize());
        output.writeObject(mapCount);

        logger.info("Writing " + mapCount + " maps...");
        m_databasestatus.setText("Saving " + mapCount + " maps...");
        for (int i = 0; i < mapCount; ++i)
          output.writeObject(m_maplist.getMapList().get(i));
        
        Integer gameTypeCount = new Integer(m_gamelist.getSize());
        output.writeObject(gameTypeCount);

        logger.info("Writing " + gameTypeCount + " games...");
        m_databasestatus.setText("Saving " + gameTypeCount + " games...");
        for (int i = 0; i < gameTypeCount; ++i)
          output.writeObject(m_gamelist.getGameList().get(i));
        
        logger.info("Writing Game Setup");
        m_databasestatus.setText("Saving Game Setup");
        output.writeObject(m_setup.getFairness());
        output.writeObject(m_setup.getGameDelay());
        output.writeObject(m_setup.getTeamCount());
        output.writeObject(m_setup.getIgnoreLastTeam());
        for (int i = 0; i < GameSetup.MAX_ALLOWED_TEAMS; ++i)
        {
          output.writeObject(m_setup.getTeamCap(i));
          output.writeObject(m_setup.getTeamName(i));
        }
        
        logger.info("Database written successfully");
        m_databasestatus.setText("Database saved successfully");
      }
      finally
      {
        output.close();
      }
    }
    catch (IOException ex)
    {
      logger.log(Level.SEVERE, "Cannot perform output.", ex);
    }
  }

  /** Set the players */
  public void setPlayerList(PlayerList players)
  {
    m_playerlist = players;
    updatePlayers();
  }
  
  public void setGameList(GameList games)
  {
    m_gamelist = games;
    updateGames();
  }
  
  public void setMapList(MapList maps)
  {
    m_maplist = maps;
    updateMaps();
  }

  private void updatePlayers()
  {
    if (m_playerlistview == null)
      return;
    m_playerlistview.setModel(m_playerlist);
    m_playerlistview.invalidate();
  }

  private void updateGames()
  {
    if (m_gamelistview == null)
      return;
    m_gamelistview.setModel(m_gamelist);
    m_gamelistview.invalidate();
    
    JPanel gameSettings = new JPanel();
    gameSettings.setLayout(new BoxLayout(gameSettings, BoxLayout.PAGE_AXIS));
    
    for (int i = 0; i < m_gamelist.getSize(); ++i)
    {
      JPanel game = new JPanel();
      GameList.GameType type = (GameList.GameType)m_gamelist.getElementAt(i);
      game.setBorder(new TitledBorder(type.name+" Settings"));
      game.setFocusable(true);
      game.setLayout(new FlowLayout());
      game.add(new JLabel("Weight:"));
      JSlider weight = new JSlider(JSlider.HORIZONTAL, 0, 10, 0);
        weight.setMajorTickSpacing(5);
        weight.setMinorTickSpacing(1);
        weight.setPaintTicks(true);
        weight.setSnapToTicks(true);
        weight.setPaintLabels(true);
        weight.setValue(type.weight);
        weight.setName("Game Weight:"+i);
        weight.addChangeListener(this);
        weight.addKeyListener(this);
      game.add(weight);
      game.add(Box.createHorizontalStrut(36));
      game.add(new JLabel("Enabled:"));
      JCheckBox enabled = new JCheckBox();
        enabled.setName("Game Enabled:"+i);
        enabled.setSelected(type.enabled);
        enabled.addItemListener(this);
        enabled.addKeyListener(this);
      game.add(enabled);
      gameSettings.add(game);
    }
    
    m_gamesettingspane.getViewport().setView(gameSettings);
  }
  
  private void updateMaps()
  {
    if (m_maplistview == null)
      return;
    m_maplistview.setModel(m_maplist);
    m_maplistview.invalidate();
    
    JPanel mapSettings = new JPanel();
    mapSettings.setLayout(new BoxLayout(mapSettings, BoxLayout.PAGE_AXIS));
    
    for (int i = 0; i < m_maplist.getSize(); ++i)
    {
      JPanel panel = new JPanel();
      JPanel sub1 = new JPanel();
      JPanel sub2 = new JPanel();
      MapList.Map map = (MapList.Map)m_maplist.getElementAt(i);
      panel.setBorder(new TitledBorder(map.name+" Settings"));
      panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
      sub1.setLayout(new FlowLayout());
      sub2.setLayout(new FlowLayout());
      panel.add(sub1);
      panel.add(sub2);
      sub1.add(new JLabel("Min:"));
      JSlider min = new JSlider(JSlider.HORIZONTAL, 2, 16, map.min_players);
        min.setMajorTickSpacing(2);
        min.setMinorTickSpacing(1);
        min.setPaintTicks(true);
        min.setPaintLabels(true);
        min.setSnapToTicks(true);
        min.setName("Min Players:"+i);
        min.addChangeListener(this);
        min.addKeyListener(this);
      sub1.add(min);
      sub1.add(new JLabel("Max:"));
      JSlider max = new JSlider(JSlider.HORIZONTAL, 2, 16, map.max_players);
        max.setMajorTickSpacing(2);
        max.setMinorTickSpacing(1);
        max.setPaintTicks(true);
        max.setPaintLabels(true);
        min.setSnapToTicks(true);
        max.setName("Max Players:"+i);
        max.addChangeListener(this);
        max.addKeyListener(this);
      sub1.add(max);
      sub1.add(Box.createHorizontalStrut(36));
      sub2.add(new JLabel("Weight:"));
      JSlider weight = new JSlider(JSlider.HORIZONTAL, 0, 5, map.weight);
        weight.setMajorTickSpacing(1);
        weight.setMinorTickSpacing(1);
        weight.setPaintTicks(true);
        weight.setPaintLabels(true);
        weight.setSnapToTicks(true);
        weight.setName("Map Weight:"+i);
        weight.addChangeListener(this);
        weight.addKeyListener(this);
      sub2.add(weight);
      sub2.add(Box.createHorizontalStrut(36));
      sub2.add(new JLabel("Enabled:"));
      JCheckBox enabled = new JCheckBox();
        enabled.setName("Map Enabled:"+i);
        enabled.setSelected(map.enabled);
        enabled.addItemListener(this);
        enabled.addKeyListener(this);
        enabled.setSelected(map.enabled);
      sub2.add(enabled);
      panel.validate();
      mapSettings.add(panel);
    }
    
    m_mapsettingspane.getViewport().setView(mapSettings);
  }

  /* Constructor helpers */
  private void createWindow()
  {
    m_fsframe = new JFrame();
    m_fsframe.setResizable(false);
    m_fsframe.addKeyListener(this);
    m_fsframe.setName("Main Window");
    try {
      UIManager.setLookAndFeel(
        UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception e) {
    }
  }
  
  private void makeFullScreen()
  {
    if (!m_fsframe.isDisplayable()) m_fsframe.setUndecorated(true);
    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    gd.setFullScreenWindow(m_fsframe);
  }
  
  private void showWindow()
  {
    m_fsframe.setVisible(true);
  }
  
  /**
   * 
   */
  private void createContents()
  {
    // Use these!
    int screenWidth = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    int screenHeight = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
    
    /* * * * * * * * */
    /* Main Display  */
    /* * * * * * * * */
    
    // Top level
    //BorderLayout layout = new BorderLayout();
    SpringLayout layout = new SpringLayout();
    JPanel contentPane = new JPanel(layout);
    contentPane.setBackground(Color.BLACK);
    contentPane.setBorder(new LineBorder(Color.BLACK, 10));
 
    // Low Level
    m_maplabel = new JLabel();
      //m_maplabel.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage("res/Rat's_Nest.jpg")));
      m_maplabel.setText("Please Wait...");
      m_maplabel.setVerticalTextPosition(JLabel.TOP);
      m_maplabel.setHorizontalTextPosition(JLabel.CENTER);
      m_maplabel.setForeground(Color.WHITE);
      m_maplabel.setFont(new Font("Serif", Font.BOLD, screenHeight/20));
    contentPane.add(m_maplabel);
    
    layout.getConstraints(m_maplabel).setX(
        Spring.sum(
            Spring.constant(screenWidth/2),
            Spring.minus(Spring.scale(layout.getConstraints(m_maplabel).getWidth(), 0.5f))
        )
    );
    layout.putConstraint(SpringLayout.SOUTH, m_maplabel, -25, SpringLayout.SOUTH, contentPane);
    
    m_teamgrid = new TeamGrid();
      m_teamgrid.apply();
      m_teamgrid.setOpaque(false);
    contentPane.add(m_teamgrid);
   
    layout.getConstraints(m_teamgrid).setWidth(
        Spring.constant(screenWidth*4/5)
    );
    layout.getConstraints(m_teamgrid).setHeight(
        Spring.constant(screenHeight*2/9)
    );
    layout.getConstraints(m_teamgrid).setX(
        Spring.sum(
            Spring.constant(screenWidth/2),
            Spring.minus(Spring.scale(layout.getConstraints(m_teamgrid).getWidth(), 0.5f))
        )
    );
    layout.putConstraint(SpringLayout.NORTH, m_teamgrid, screenHeight/15, SpringLayout.NORTH, contentPane);
    
    
    m_gametime = new JProgressBar();
      m_gametime.setMaximum(m_setup.getGameDelay());
      m_gametime.setValue(m_setup.getGameDelay());
      m_gametime.setStringPainted(true);
      m_gametime.setString("Game Time!");
      m_gametime.setFont(new Font("Serif", Font.BOLD, screenHeight/30));
      m_gametime.addPropertyChangeListener(this);
    contentPane.add(m_gametime);
    
    layout.putConstraint(SpringLayout.NORTH, m_gametime, 25, SpringLayout.SOUTH, m_teamgrid);
    layout.getConstraints(m_gametime).setWidth(Spring.constant(screenWidth/2));
    layout.getConstraints(m_gametime).setHeight(Spring.constant(screenHeight/25));
    layout.getConstraints(m_gametime).setX(
        Spring.sum(
            Spring.constant(screenWidth/2),
            Spring.minus(Spring.scale(layout.getConstraints(m_gametime).getWidth(), 0.5f))
        )
    );
    
    // Mid Level
    //contentPane.add(playerPane, BorderLayout.PAGE_START); 
    //contentPane.add(img, BorderLayout.CENTER);
    contentPane.validate();

    m_maincontent = contentPane;
    //m_fsframe.setContentPane(m_maincontent);
    
    /* * * * * * * * */
    /* Player Editor */
    /* * * * * * * * */
    
    // Create the two sides of the split pane
    JScrollPane playerListPane = new JScrollPane();
    JScrollPane playerSettingsPane = new JScrollPane();
    JSplitPane playerEditPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, playerListPane, playerSettingsPane);
    playerEditPane.setDividerLocation(FRAME_MIN_SIZE);
    playerEditPane.setBorder(new TitledBorder("Player Setup"));

    //Provide minimum sizes for the two components in the split pane
    Dimension minimumSize = new Dimension(FRAME_MIN_SIZE, 50);
    playerListPane.setMinimumSize(minimumSize);
    playerSettingsPane.setMinimumSize(minimumSize);

    // Create the two panes that go inside the scroll panes and set them
    JList playerList = new JList();
    JPanel playerSettings = new JPanel();
    playerListPane.getViewport().setView(playerList);
    playerSettingsPane.getViewport().setView(playerSettings);
    
    // Set up the playerList
    playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    if (m_playerlist == null)
      m_playerlist = new PlayerList();
    playerList.setModel(m_playerlist);
    playerList.setName("Player List");
    playerList.addListSelectionListener(this);
    playerList.addKeyListener(this);
    
    // Set up the playerSettings
    //playerSettings.setLayout(new BoxLayout(playerSettings, BoxLayout.PAGE_AXIS));
    playerSettings.setLayout(new BorderLayout());
    JPanel settingsGroup = new JPanel();
    settingsGroup.setLayout(new BoxLayout(settingsGroup, BoxLayout.PAGE_AXIS));
    playerSettings.add(settingsGroup, BorderLayout.PAGE_START);
    
    // Name and GamerTag
    JPanel namePanel = new JPanel();
      namePanel.setBorder(new TitledBorder("Player Name and Gamer Tag"));
      
      JTextField inputPlayerName = new JTextField(32);
      inputPlayerName.setName("Player Name");
      inputPlayerName.getDocument().putProperty("Name", "Player Name");
      inputPlayerName.getDocument().addDocumentListener(this);
      inputPlayerName.addKeyListener(this);
      
      JLabel labelOpenParen = new JLabel("(");
      
      JTextField inputGamerTag = new JTextField(12);
      inputGamerTag.setName("Player Name");
      inputGamerTag.getDocument().putProperty("Name", "Gamer Tag");
      inputGamerTag.getDocument().addDocumentListener(this);
      inputGamerTag.addKeyListener(this);
      
      JLabel labelCloseParen = new JLabel(")");
      
      namePanel.add(inputPlayerName);
      namePanel.add(labelOpenParen);
      namePanel.add(inputGamerTag);
      namePanel.add(labelCloseParen);
    settingsGroup.add(namePanel);
    
    JPanel skillPanel = new JPanel();
      skillPanel.setBorder(new TitledBorder("Player Skill Level"));
      JSlider inputSkill = new JSlider(JSlider.HORIZONTAL, 0, 10, 0);
        inputSkill.setMajorTickSpacing(5);
        inputSkill.setMinorTickSpacing(1);
        inputSkill.setPaintTicks(true);
        inputSkill.setPaintLabels(true);
        inputSkill.setSnapToTicks(true);
        inputSkill.setName("Player Skill");
        inputSkill.addChangeListener(this);
        inputSkill.addKeyListener(this);
      skillPanel.add(inputSkill);
    settingsGroup.add(skillPanel);
    
    JPanel keyturnPanel = new JPanel();
      keyturnPanel.setBorder(new TitledBorder("Override Passcode and Major Player"));
      JPasswordField majorPasscode = new JPasswordField(12);
        majorPasscode.setName("Passcode");
        majorPasscode.getDocument().putProperty("Name", "Passcode");
        majorPasscode.getDocument().addDocumentListener(this);
        majorPasscode.addKeyListener(this);
      keyturnPanel.add(majorPasscode);
      JCheckBox majorButton = new JCheckBox();
        majorButton.setName("Major Player");
        majorButton.addItemListener(this);
        majorButton.addKeyListener(this);
      keyturnPanel.add(majorButton);
    settingsGroup.add(keyturnPanel);
    
    JPanel enabledPanel = new JPanel();
      enabledPanel.setBorder(new TitledBorder("Is this gamer playing?"));
      JCheckBox enableButton = new JCheckBox("Playing");
        enableButton.setName("Playing");
        enableButton.addItemListener(this);
        enableButton.addKeyListener(this);
      enabledPanel.add(enableButton);
    settingsGroup.add(enabledPanel);
    
    JPanel newPlayerPanel = new JPanel();
      newPlayerPanel.setBorder(BorderFactory.createTitledBorder("New Player Creation and Player Deletion"));
      JButton createDefault = new JButton("New Player");
        createDefault.setName("Create Default");
        createDefault.addKeyListener(this);
        createDefault.addActionListener(this);
      newPlayerPanel.add(createDefault);
      JButton createDuplicate = new JButton("Duplicate Player");
        createDuplicate.setName("Create Duplicate");
        createDuplicate.addKeyListener(this);
        createDuplicate.addActionListener(this);
      newPlayerPanel.add(createDuplicate);
      newPlayerPanel.add(Box.createHorizontalStrut(32));
      JButton deletePlayer = new JButton("Delete Player");
        deletePlayer.setName("Delete Player");
        deletePlayer.addKeyListener(this);
        deletePlayer.addActionListener(this);
      newPlayerPanel.add(deletePlayer);
      JCheckBox reallyDelete = new JCheckBox();
        reallyDelete.setName("Really Delete");
        reallyDelete.addKeyListener(this);
        m_delete_confirm = reallyDelete;
      newPlayerPanel.add(reallyDelete);
    playerSettings.add(newPlayerPanel, BorderLayout.PAGE_END);
    
    /* Player Editor */
    m_active_gamertag = inputGamerTag;
    m_active_playername = inputPlayerName;
    m_active_skill = inputSkill;
    m_active_enabled = enableButton;
    m_playereditor = playerEditPane;
    m_playerlistview = playerList;
    m_vetopower = majorButton;
    m_vetopasscode = majorPasscode;
    m_fsframe.setContentPane(m_maincontent);
    
    /* * * * * * * */
    /* Game Editor */
    /* * * * * * * */
    
    // Create the two sides of the upper split pane
    JScrollPane gameListPane = new JScrollPane();
    JScrollPane gameSettingsPane = new JScrollPane();
    JSplitPane gameEditPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gameListPane, gameSettingsPane);
    gameEditPane.setDividerLocation(FRAME_MIN_SIZE);
    gameEditPane.setBorder(new TitledBorder("Game Setup"));
    
    // Create the two sides of the lower split pane
    JScrollPane mapListPane = new JScrollPane();
    JScrollPane mapSettingsPane = new JScrollPane();
    JSplitPane mapEditPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapListPane, mapSettingsPane);
    mapEditPane.setDividerLocation(FRAME_MIN_SIZE);
    mapEditPane.setBorder(new TitledBorder("Map Setup"));
    
    // Create the top and bottom half of the split pane
    JSplitPane mainGameEditPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gameEditPane, mapEditPane);
    mainGameEditPane.setDividerLocation((int)Toolkit.getDefaultToolkit().getScreenSize().height/2);

    //Provide minimum sizes for the two components in the split pane
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension minGameEditorSize = new Dimension(screenSize.height/2, FRAME_MIN_SIZE);
    gameListPane.setMinimumSize(minGameEditorSize);
    gameSettingsPane.setMinimumSize(minGameEditorSize);
    mapListPane.setMinimumSize(minGameEditorSize);
    mapSettingsPane.setMinimumSize(minGameEditorSize);

    // Create the two panes that go inside the scroll panes and set them
    JList gameList = new JList();
    gameListPane.getViewport().setView(gameList);
    JList mapList = new JList();
    mapListPane.getViewport().setView(mapList);
    
    // Set up the gameList
    gameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    if (m_gamelist == null)
      m_gamelist = new GameList();
    gameList.setModel(m_gamelist);
    gameList.setName("Game List");
    gameList.addListSelectionListener(this);
    gameList.addKeyListener(this);
    
    // Set up the mapList
    mapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    if (m_maplist == null)
      m_maplist = new MapList();
    mapList.setModel(m_maplist);
    mapList.setName("Map List");
    mapList.addListSelectionListener(this);
    mapList.addKeyListener(this);
    
    m_gamesettingspane = gameSettingsPane;
    m_gamelistview = gameList;
    m_mapsettingspane = mapSettingsPane;
    m_maplistview = mapList;
    m_gameeditor = mainGameEditPane;
    updateGames(); // create the game list settings
    updateMaps();
    
    /* * * * * * * * *
     * Setup Editor  *
     * * * * * * * * */
    
    JScrollPane setupPane = new JScrollPane();
    JPanel setupLayoutHelper = new JPanel();
    setupLayoutHelper.setLayout(new BorderLayout());
    setupLayoutHelper.setBorder(new TitledBorder("Setup"));
    setupPane.getViewport().setView(setupLayoutHelper);

    JPanel setupPanel = new JPanel();
    setupPanel.setLayout(new BoxLayout(setupPanel, BoxLayout.PAGE_AXIS));
    setupLayoutHelper.add(setupPanel, BorderLayout.PAGE_START);
    
    JPanel teamCountPanel = new JPanel();
      teamCountPanel.setBorder(new TitledBorder("Team Generation"));
      teamCountPanel.setLayout(new FlowLayout());
      teamCountPanel.add(new JLabel("How many teams should I generate?"));
      JSlider teamCount = new JSlider(JSlider.HORIZONTAL, 2, GameSetup.MAX_ALLOWED_TEAMS, m_setup.getTeamCount());
        teamCount.setMajorTickSpacing(1);
        teamCount.setMinorTickSpacing(1);
        teamCount.setPaintTicks(true);
        teamCount.setPaintLabels(true);
        teamCount.setSnapToTicks(true);
        teamCount.setName("Team Count");
        teamCount.addKeyListener(this);
        teamCount.addChangeListener(this);
      teamCountPanel.add(teamCount);
    setupPanel.add(teamCountPanel);
    
    JPanel fairnessPanel = new JPanel();
      fairnessPanel.setBorder(new TitledBorder("Player Allocation"));
      fairnessPanel.setLayout(new FlowLayout());
      fairnessPanel.add(new JLabel("How fair should the teams be?"));
      JSlider fairness = new JSlider(JSlider.HORIZONTAL, 0, 50, m_setup.getFairness());
        fairness.setMajorTickSpacing(10);
        fairness.setMinorTickSpacing(5);
        fairness.setPaintTicks(true);
        fairness.setPaintLabels(true);
        fairness.setSnapToTicks(true);
        fairness.setName("Fairness");
        fairness.addKeyListener(this);
        fairness.addChangeListener(this);
      fairnessPanel.add(fairness);
      JCheckBox ignoreLast = new JCheckBox("Ignore last team");
        ignoreLast.setName("Ignore Last");
        ignoreLast.addItemListener(this);
        ignoreLast.addKeyListener(this);
        ignoreLast.setSelected(m_setup.getIgnoreLastTeam());
      fairnessPanel.add(ignoreLast);
    setupPanel.add(fairnessPanel);
    
    JPanel delayPanel = new JPanel();
      delayPanel.setBorder(new TitledBorder("Time Between Games"));
      delayPanel.setLayout(new FlowLayout());
      delayPanel.add(new JLabel("How many minutes between games?"));
      JSlider delaySlider = new JSlider(JSlider.HORIZONTAL, 0, 60*32, m_setup.getGameDelay());
        delaySlider.setMajorTickSpacing(5*60);
        delaySlider.setMinorTickSpacing(30);
        delaySlider.setPaintTicks(true);
        delaySlider.setPaintLabels(true);
        delaySlider.setSnapToTicks(true);
        delaySlider.setName("Game Delay");
        delaySlider.addKeyListener(this);
        delaySlider.addChangeListener(this);
        delaySlider.setPreferredSize(new Dimension(500, delaySlider.getPreferredSize().height));
        Hashtable<Integer,JComponent> labels = new Hashtable<Integer,JComponent>();
        labels.put(60*0, new JLabel("0"));
        labels.put(60*1, new JLabel("1"));
        labels.put(60*2, new JLabel("2"));
        labels.put(60*3, new JLabel("3"));
        labels.put(60*4, new JLabel("4"));
        labels.put(60*5, new JLabel("5"));
        labels.put(60*6, new JLabel("6"));
        labels.put(60*7, new JLabel("7"));
        labels.put(60*8, new JLabel("8"));
        labels.put(60*9, new JLabel("9"));
        labels.put(60*10, new JLabel("10"));
        labels.put(60*12, new JLabel("12"));
        labels.put(60*14, new JLabel("14"));
        labels.put(60*16, new JLabel("16"));
        labels.put(60*18, new JLabel("18"));
        labels.put(60*20, new JLabel("20"));
        labels.put(60*24, new JLabel("24"));
        labels.put(60*28, new JLabel("28"));
        labels.put(60*32, new JLabel("32"));
        delaySlider.setLabelTable(labels);
      delayPanel.add(delaySlider);
    setupPanel.add(delayPanel);
    
    JPanel teamNamePanel = new JPanel();
      teamNamePanel.setBorder(new TitledBorder("Team Names"));
      teamNamePanel.setLayout(new BoxLayout(teamNamePanel, BoxLayout.PAGE_AXIS));
      JPanel subNamePanel1 = new JPanel();
      subNamePanel1.add(new JLabel("1-4"));
      for (int i = 0; i < GameSetup.MAX_ALLOWED_TEAMS/2; ++i)
      {
        JTextField fld = new JTextField(12);
        fld.setText(m_setup.getTeamName(i));
        fld.setName("Team Name:" + i);
        fld.getDocument().putProperty("Name", "Team Name:"+i);
        fld.getDocument().addDocumentListener(this);
        fld.addKeyListener(this);
        subNamePanel1.add(fld);
      }
      JPanel subNamePanel2 = new JPanel();
      subNamePanel2.add(new JLabel("5-8"));
      for (int i = GameSetup.MAX_ALLOWED_TEAMS/2; i < GameSetup.MAX_ALLOWED_TEAMS; ++i)
      {
        JTextField fld = new JTextField(12);
        fld.setText(m_setup.getTeamName(i));
        fld.setName("Team Name:" + i);
        fld.getDocument().putProperty("Name", "Team Name:"+i);
        fld.getDocument().addDocumentListener(this);
        fld.addKeyListener(this);
        subNamePanel2.add(fld);
      }
      teamNamePanel.add(subNamePanel1);
      teamNamePanel.add(subNamePanel2);
    setupPanel.add(teamNamePanel);
    
    JPanel teamCapPanel = new JPanel();
      teamCapPanel.setBorder(new TitledBorder("Team Capacities (zero disables)"));
      teamCapPanel.setLayout(new BoxLayout(teamCapPanel, BoxLayout.PAGE_AXIS));
      JPanel subCapPanel1 = new JPanel();
      subCapPanel1.add(new JLabel("1-4"));
      for (int i = 0; i < GameSetup.MAX_ALLOWED_TEAMS/2; ++i)
      {
        JSlider cap = new JSlider(JSlider.HORIZONTAL, 0, 8, m_setup.getTeamCap(i));
        cap.setMajorTickSpacing(2);
        cap.setMinorTickSpacing(1);
        cap.setPaintTicks(true);
        cap.setPaintLabels(true);
        cap.setSnapToTicks(true);
        cap.setName("Team Cap:"+i);
        cap.addKeyListener(this);
        cap.addChangeListener(this);
        cap.setPreferredSize(new Dimension(120,cap.getPreferredSize().height));
        subCapPanel1.add(cap);
      }
      JPanel subCapPanel2 = new JPanel();
      subCapPanel2.add(new JLabel("5-8"));
      for (int i = GameSetup.MAX_ALLOWED_TEAMS/2; i < GameSetup.MAX_ALLOWED_TEAMS; ++i)
      {
        JSlider cap = new JSlider(JSlider.HORIZONTAL, 0, 8, m_setup.getTeamCap(i));
        cap.setMajorTickSpacing(2);
        cap.setMinorTickSpacing(1);
        cap.setPaintTicks(true);
        cap.setPaintLabels(true);
        cap.setSnapToTicks(true);
        cap.setName("Team Cap:"+i);
        cap.addKeyListener(this);
        cap.addChangeListener(this);
        cap.setPreferredSize(new Dimension(120,cap.getPreferredSize().height));
        subCapPanel2.add(cap);
      }
      teamCapPanel.add(subCapPanel1);
      teamCapPanel.add(subCapPanel2);
    setupPanel.add(teamCapPanel);
    
    JPanel databasePanel = new JPanel();
    databasePanel.setBorder(BorderFactory.createTitledBorder("Save/Load Database"));
      JTextField databaseName = new JTextField(32);
        databaseName.setText("Halo3.dat");
        databaseName.addKeyListener(this);
        m_databasename = databaseName;
      databasePanel.add(databaseName);
      JButton saveDatabase = new JButton("Save Database");
        saveDatabase.setName("Save Database");
        saveDatabase.addActionListener(this);
        saveDatabase.addKeyListener(this);
      databasePanel.add(saveDatabase);
      JButton loadDatabase = new JButton("Load Database");
        loadDatabase.setName("Load Database");
        loadDatabase.addActionListener(this);
        loadDatabase.addKeyListener(this);
        loadDatabase.setEnabled(false); // TODO: This needs to completely refresh the UI, so it's disabled.
        m_loaddbbutton = loadDatabase;
      databasePanel.add(loadDatabase);
      if (m_databasestatus == null)
        m_databasestatus = new JLabel("Database Status Unknown");
      databasePanel.add(m_databasestatus);
    setupPanel.add(databasePanel);
      
    m_setupeditor = setupPane;
    
    /* * * * * * * * * * * */
    /* Authentication Pane */
    /* * * * * * * * * * * */
    JPanel authWrapper = new JPanel();
    JPanel authPane = new JPanel();
    authPane.setLayout(new BorderLayout());
    
    JPanel authPasscodePanel = new JPanel();
    authPasscodePanel.setBorder(BorderFactory.createTitledBorder("Enter your passcode"));
    JPasswordField enterPasscodeField = new JPasswordField(12);
      enterPasscodeField.setName("Authenticate");
      enterPasscodeField.getDocument().putProperty("Name", "Authenticate");
      enterPasscodeField.getDocument().addDocumentListener(this);
      enterPasscodeField.addKeyListener(this);
      m_authpassfield = enterPasscodeField;
    authPasscodePanel.add(enterPasscodeField);
    
    authPane.add(authPasscodePanel, BorderLayout.PAGE_START);
    authWrapper.add(authPane);
    m_authenticate = authWrapper;
  }

  /** Handle the key-pressed event from the text field. */
  public void keyPressed(KeyEvent e) 
  {
    /* Process these keys always */
    /*{
      System.out.println("Help:");
      System.out.println("  F1  Esc  - Display Game Screen or Help (this)");
      System.out.println("  F2  P    - Launch Player Editor");
      System.out.println("  F3  G    - Launch Game/Map Editor");
      System.out.println("      Q    - Quit");
      System.exit(0);
    }*/
    switch (e.getKeyCode())
    {
      /* Undocumented feature: Press F12 to minimize from fullscreen, M to restore fullscreen */
      case KeyEvent.VK_F12:
        if (m_fsframe.getContentPane() == m_maincontent)
        {
          GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
          gd.setFullScreenWindow(null);
          //if (!m_fsframe.isDisplayable()) m_fsframe.setUndecorated(false);
          m_fsframe.setState(JFrame.ICONIFIED);
        }

        /* Undocumented feature: Press F12 enable the load database button (backdoor into editing a database) */
        if (m_fsframe.getContentPane() == m_setupeditor)
        {
          logger.severe("Backdoor database load enabled!");
          m_loaddbbutton.setEnabled(true);
        }
        break;
      case KeyEvent.VK_M:
        if (m_fsframe.getContentPane() == m_maincontent)
          makeFullScreen();
        break;
      
      case KeyEvent.VK_F1:
        logger.info("Return to Game Screen");
        m_fsframe.setFocusable(true);
        m_fsframe.transferFocus(); // Needed to still receive keyboard events
        m_fsframe.setContentPane(m_maincontent);
        m_fsframe.setVisible(true);
        break;
      
      case KeyEvent.VK_P:
        if (m_fsframe.getContentPane() != m_maincontent)
          break;
      case KeyEvent.VK_F2:
        logger.info("Launching Player Editor");
        authFrameSwitch(m_playereditor, false);
        break;
      
      case KeyEvent.VK_G:
        if (m_fsframe.getContentPane() != m_maincontent)
          break;
      case KeyEvent.VK_F3:
        logger.info("Launching Game Editor");
        authFrameSwitch(m_gameeditor, false);
        break;
      
      case KeyEvent.VK_S:
        if (m_fsframe.getContentPane() != m_maincontent)
          break;
      case KeyEvent.VK_F4:
        logger.info("Launching Setup Editor");
        authFrameSwitch(m_setupeditor, false);
        break;
      
      case KeyEvent.VK_SPACE:
        if (m_fsframe.getFocusOwner().getName() == null) break;
        if (m_fsframe.getFocusOwner().getName().equals("Player List") && m_active_player != null)
        {
          int idx = m_playerlist.findPlayer(m_active_player);
          PlayerList.Player p = m_playerlist.getPlayers(false).get(idx);
          p.playing ^= true;
          m_playerlist.updatePlayer(idx, p);
          this.m_active_enabled.setSelected(p.playing);
        }
        break;
      
      case KeyEvent.VK_Q:
        if (m_fsframe.getContentPane() != m_maincontent)
          break;
        logger.info("Quit");
        System.exit(0);
        break;
        
      default:
        logger.finest("Pressed: " + KeyEvent.getKeyText(e.getKeyCode()) + "(" + e.getKeyCode() + ")");
    }
    fireKeyListener(e.getKeyCode(), e.getID());
  }

  /**
   * @param dest
   * @param skipAuth TODO
   */
  private void authFrameSwitch(JComponent dest, boolean skipAuth)
  {
    JComponent current = (JComponent) m_fsframe.getContentPane();
    
    boolean goToFrame = false;
    if (!skipAuth && (current == m_maincontent || current == m_authenticate))
    {
      boolean activeMajors = false;
      for (int i = 0; !goToFrame && i < m_playerlist.getSize(); ++i)
      {
        if (m_playerlist.getPlayers().get(i).veto_power && m_playerlist.getPlayers().get(i).passcode_hash.length() > 0)
          activeMajors = true;
      }
      if (!activeMajors)
        goToFrame = true;
    }
    else
      goToFrame = true;
    
    if (!goToFrame)
    {
      m_pendingscreen = dest;
      dest = m_authenticate;
    }
    else
      m_pendingscreen = null;
    m_fsframe.setFocusable(true);
    m_fsframe.transferFocus(); // Needed to still receive keyboard events
    m_fsframe.setContentPane(dest);
    m_fsframe.setVisible(true);
    if (!goToFrame)
    {
      m_authpassfield.setText("");
      m_authpassfield.requestFocus();
    }
  }
  
  /** Handle the key typed event from the text field. */
  public void keyTyped(KeyEvent e) {
    //String name = KeyEvent.getKeyText(e.getKeyCode());
    //System.out.println("Typed: " + name);
  }

  /** Handle the key-released event from the text field. */
  public void keyReleased(KeyEvent e) {
    //String name = KeyEvent.getKeyText(e.getKeyCode());
    //System.out.println("Released: " + name + "(" + e.getKeyCode() + ")");
    fireKeyListener(e.getKeyCode(), e.getID());
  }

  public void stateChanged(ChangeEvent e)
  {
    JComponent src = (JComponent)e.getSource();
    Container frame = m_fsframe.getContentPane();
    // getStateIsAdjusting
    if (src.getClass() == JSlider.class && ((JSlider)src).getValueIsAdjusting())
      return;
    if (src.getName() == null)
      return;
    if (frame == m_playereditor && m_active_player != null)
    {
      int idx = m_playerlist.findPlayer(m_active_player);
      logger.info("Component: " + src.getName());
      if (src.getName().equals("Player Skill") || src.getName().equals("Player Priority"))
      {
        int newValue = ((JSlider)src).getValue();
        logger.info("New Value: " + newValue);
        PlayerList.Player active = m_active_player;
        if (src.getName().equals("Player Skill"))
          active.skill = newValue;
        m_playerlist.updatePlayer(idx, active);
      }
    }
    else if (frame == m_gameeditor)
    {
      logger.info("Component: " + src.getName());
      
      int idx = parseSettingIndex(src.getName());
      String name = parseSettingName(src.getName());
      
      int newValue = ((JSlider)src).getValue();
      logger.info("New Value: " + newValue);
      // Game Weight
      if (name.equals("Game Weight"))
      {
        GameList.GameType updated = m_gamelist.getGameList().get(idx);
        updated.weight = newValue;
        m_gamelist.updateGame(idx, updated);
      }
      else if (name.equals("Min Players"))
      {
        MapList.Map updated = m_maplist.getMapList().get(idx);
        updated.min_players = newValue;
        m_maplist.updateMap(idx, updated);
      }
      else if (name.equals("Max Players"))
      {
        MapList.Map updated = m_maplist.getMapList().get(idx);
        updated.max_players = newValue;
        m_maplist.updateMap(idx, updated);
      }
      else if (name.equals("Map Weight"))
      {
        MapList.Map updated = m_maplist.getMapList().get(idx);
        updated.weight = newValue;
        m_maplist.updateMap(idx, updated);
      }
      else
        logger.severe("Unrecognized Setting: " + name);
    }
    else if (frame == m_setupeditor)
    {
      logger.info("Component: " + src.getName());
      
      String name = parseSettingName(src.getName());
      int index = parseSettingIndex(src.getName());
      
      int newValue = ((JSlider)src).getValue();
      logger.info("New Value: " + newValue);
      // Game Weight
      if (name.equals("Team Count"))
      {
        m_setup.setTeamCount(newValue);
      }
      else if (name.equals("Fairness"))
      {
        m_setup.setFairness(newValue);
      }
      else if (name.equals("Team Cap"))
      {
        m_setup.setTeamCap(index, newValue);
      }
      else if (name.equals("Game Delay"))
      {
        m_setup.setGameDelay(newValue);
      }
      else
        logger.severe("Unrecognized Setting: " + name);
    }
  }

  private int parseSettingIndex(String srcname)
  {
    int colon = srcname.indexOf(':');
    int idx = -1;
    if (colon >= 0)
      idx = Integer.parseInt(srcname.substring(colon+1));
    return idx;
  }

  private String parseSettingName(String srcname)
  {
    int colon = srcname.indexOf(':');
    String name = srcname;
    if (colon >= 0)
      name = srcname.substring(0, colon);
    return name;
  }

  public void actionPerformed(ActionEvent e)
  {
    JComponent src = (JComponent)e.getSource();
    logger.info("Action: " + src.getName());
    if (src.getName() == null)
      return;
    String name = src.getName();
    if (name.equals("Create Default"))
    {
      m_active_player = null;
      m_playerlistview.setSelectedIndices(new int[0]);
      PlayerList.Player def = new PlayerList.Player("[ New Player ]", "n00b"); // should be at end
      m_playerlist.add(def);
      m_playerlistview.invalidate();
      m_active_player = def;
      showPlayer(def);
    }
    else if (name.equals("Create Duplicate"))
    {
      if (m_active_player == null)
        return;
      PlayerList.Player def = new PlayerList.Player();
      def.gamertag = m_active_player.gamertag;
      def.name = "[ " + m_active_player.name + " ]";
      def.playing = m_active_player.playing;
      def.skill = m_active_player.skill;
      m_active_player = null;
      m_playerlistview.setSelectedIndices(new int[0]);
      m_playerlist.add(def);
      m_playerlistview.invalidate();
      m_active_player = def;
      showPlayer(def);
    }
    else if (name.equals("Delete Player"))
    {
      if (m_active_player == null)
        return;
      if (m_delete_confirm.isSelected())
      {
        PlayerList.Player active = m_active_player;
        m_active_player = null;
        logger.info("DELETE");
        showPlayer(new PlayerList.Player());
        m_playerlistview.setSelectedIndices(new int[0]);
        m_playerlist.delete(active);
      }
      else
        m_delete_confirm.setSelected(true);
    }
    else if (name.equals("Save Database"))
    {
      m_databasestatus.setText("Please wait while we save the database...");
      saveDatabase(m_databasename.getText());
    }
    else if (name.equals("Load Database"))
    {
      m_databasestatus.setText("Please wait while we load the database...");
      loadDatabase(m_databasename.getText());
    }
    else
    {
      logger.severe("Unrecognized button: " + name);
    }
  }

  public void changedUpdate(DocumentEvent e)
  {
    try
    {
      String src = (String)e.getDocument().getProperty("Name");
      
      if (m_fsframe.getContentPane() == m_playereditor && m_active_player != null)
      {
        int idx = m_playerlist.findPlayer(m_active_player);
        logger.info("Document change: " + src);
        if (src == null)
          return;
        String newValue = e.getDocument().getText(0,e.getDocument().getLength());
        if (!src.equals("Passcode"))
          logger.info("   New Contents: " + newValue);
        else
        {
          String message = "   New Contents: ";
          for (int i = 0; i < newValue.length(); ++i)
            message += "*";
          logger.info(message);
        }
        
        PlayerList.Player active = m_active_player;
        if (src.equals("Player Name"))
          active.name = newValue;
        else if (src.equals("Gamer Tag"))
          active.gamertag = newValue;
        else if (src.equals("Passcode"))
          active.setPasscode(newValue);
        m_playerlist.updatePlayer(idx, active);
      }
      else if (m_fsframe.getContentPane() == m_setupeditor)
      {
        if (src == null)
          return;
        String setting = parseSettingName(src);
        int index = parseSettingIndex(src);
        String newValue = e.getDocument().getText(0,e.getDocument().getLength());
        logger.info("Document change: " + setting + "[" + index + "] = " + newValue);
        if (setting.equals("Team Name") && index >= 0 && index < GameSetup.MAX_ALLOWED_TEAMS)
          m_setup.setTeamName(index, newValue);
      }
      else if (m_fsframe.getContentPane() == m_authenticate)
      {
        if (src == null)
          return;
        String setting = parseSettingName(src);
        String newValue = e.getDocument().getText(0,e.getDocument().getLength());
        logger.info("Document change: " + setting);
        if (setting != "Authenticate")
        {
          logger.severe("Misdirected events!  Only Authentication methods should propagate here.");
          return;
        }
        // TODO
        for (int i = 0; i < m_playerlist.getSize(); ++i)
        {
          PlayerList.Player p = m_playerlist.getPlayers().get(i);
          if (p.veto_power && p.passcode_hash.length() > 0)
            if (p.checkPasscode(newValue))
            {
              authFrameSwitch(m_pendingscreen, true);
            }
        }
      }
    }
    catch (BadLocationException e1)
    {
      logger.log(Level.SEVERE, "Bad Location", e1); //e1.printStackTrace();
    }
  }

  public void insertUpdate(DocumentEvent e)
  {
    changedUpdate(e);
  }

  public void removeUpdate(DocumentEvent e)
  {
    changedUpdate(e);
  }

  public void valueChanged(ListSelectionEvent e)
  {
    JComponent src = (JComponent)e.getSource();
    if (src.getName() == null)
      return;
    if (src.getClass() == JList.class && ((JList)src).getValueIsAdjusting() == true)
      return;
    if (src.getName().equals("Player List"))
    {
      logger.info("Value changed: " + src.getName());
      JList srcList = (JList)src;
      m_active_player = null;
      int index = srcList.getSelectedIndex();
      if (index == -1)
      {
        logger.info("     Selected: (none)");
        return;
      }
      PlayerList.Player active = this.m_playerlist.getPlayers().get(index);
      showPlayer(active);
      m_active_player = active;
      logger.info("     Selected: " + m_active_player);
    }
  }

  /**
   * Show the info for the given player in the player settings pane
   * @param active
   */
  private void showPlayer(PlayerList.Player active)
  {
    this.m_active_gamertag.setText(active.gamertag);
    this.m_active_playername.setText(active.name);
    this.m_active_skill.setValue(active.skill);
    this.m_active_enabled.setSelected(active.playing);
    this.m_vetopower.setSelected(active.veto_power);
    if (active.passcode_hash.length() > 0)
      this.m_vetopasscode.setText("PassCode");
    else
      this.m_vetopasscode.setText("");
    this.m_delete_confirm.setSelected(false);
  }

  public void itemStateChanged(ItemEvent e)
  {
    JComponent src = (JComponent)e.getSource();
    Container frame = m_fsframe.getContentPane();
    if (frame == m_playereditor && m_active_player != null)
    {
      logger.info("State changed: " + src.getName());
      if (m_active_player != null && src.getName().equals("Playing") || src.getName().equals("Major Player"))
      {
        int idx = m_playerlist.findPlayer(m_active_player);
        boolean newValue = e.getStateChange() == ItemEvent.SELECTED;
        logger.info("    New State: " + (newValue?"Checked":"Not Checked"));
        PlayerList.Player active = m_active_player;
        if (src.getName().equals("Playing"))
          active.playing = newValue;
        if (src.getName().equals("Major Player"))
          active.veto_power = newValue;
        m_playerlist.updatePlayer(idx, active);
      }
    }
    else if (frame == m_gameeditor)
    {
      int idx = parseSettingIndex(src.getName());
      String name = parseSettingName(src.getName());
      boolean newValue = e.getStateChange() == ItemEvent.SELECTED;
      logger.info("    New State: " + (newValue?"Checked":"Not Checked"));
      if (name.equals("Game Enabled"))
      {
        GameList.GameType updated = m_gamelist.getGameList().get(idx);
        updated.enabled = newValue;
        m_gamelist.updateGame(idx, updated);
      }
      else if (name.equals("Map Enabled"))
      {
        MapList.Map updated = m_maplist.getMapList().get(idx);
        updated.enabled = newValue;
        m_maplist.updateMap(idx, updated);
      }
    }
    else if (frame == m_setupeditor)
    {
      //int idx = parseSettingIndex(src.getName());
      String name = parseSettingName(src.getName());
      boolean newValue = e.getStateChange() == ItemEvent.SELECTED;
      logger.info("    New State: " + (newValue?"Checked":"Not Checked"));
      if (name.equals("Ignore Last"))
      {
        m_setup.setIgnoreLast(newValue);
      }
      else
        logger.warning("Unknown setting: " + name);
    }
    
  }

  public void setGame(Team[] teams, GameList.GameType game, MapList.Map map)
  {
    if (m_teamgrid == null)
      return;
    m_teamgrid.clear();
    
//    StackTraceElement[] caller = new Throwable().getStackTrace();
//    logger.info("Called from: " + caller[1].getMethodName() + " in " + caller[1].getFileName() + ":" + caller[1].getLineNumber());
//    logger.info("       from: " + caller[2].getMethodName() + " in " + caller[2].getFileName() + ":" + caller[2].getLineNumber());
//    logger.info("       from: " + caller[3].getMethodName() + " in " + caller[3].getFileName() + ":" + caller[3].getLineNumber());
    
    for (int t = 0; t < teams.length; ++t)
    {
      teams[t].setTeamName(m_setup.getTeamName(t));
      m_teamgrid.addTeam(teams[t]);
    }
    m_teamgrid.apply();

    m_maplabel.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(HaloGameMaster.class.getResource(map.image))));
    m_maplabel.setText(game + " on " + map);
    
    m_gamestarttime = System.currentTimeMillis();
    
    if (m_timerunner == null)
    {
      m_timerunner = new SwingWorker() {
        public Object construct() {
          logger.info("Worker Starting");
          m_gametime.putClientProperty("max", (int)m_setup.getGameDelay());
          m_gametime.putClientProperty("val", (int)0);
          while (true)
          {
            int elapsed = (int) ((System.currentTimeMillis() - m_gamestarttime) / 1000);
            if (elapsed <= m_setup.getGameDelay())
            {
              m_gametime.putClientProperty("val", elapsed);
              int minsleft = (int) ((m_setup.getGameDelay()-elapsed)/60);
              int secsleft = (int) ((m_setup.getGameDelay()-elapsed)%60);
              m_gametime.putClientProperty("str", "Remaining: " + minsleft + ":" + (secsleft<10?"0":"") + secsleft);
            }
            else
            {
              if (m_gametime.getString() != "Game Time!")
              {
                m_gametime.putClientProperty("max", 100);
                m_gametime.putClientProperty("val", 100);
                m_gametime.putClientProperty("str", "Game Time!");
              }
            }
            try
            {
              Thread.sleep(100);
            }
            catch (Exception e)
            {
              logger.warning("Interrupted");
            }
          }
         }
         public void finished() {
            logger.severe("Time Runner exited!");
         }
      };
      m_timerunner.start();
    }
    
    m_fsframe.repaint();
    logger.info("Game successfully set");
  }
  
  void addKeyListener(KeyListener l)
  {
    m_fskeylisteners.add(l);
  }
  
  void removeKeyListener(KeyListener l)
  {
    m_fskeylisteners.remove(l);
  }
  
  void fireKeyListener(int keyCode, int eventID)
  {
    for (int i = 0; i < m_fskeylisteners.size(); ++i)
    {
      m_fskeylisteners.get(i).MainScreenKeyHit(keyCode, eventID);
    }
  }

  /** This should be set before the components are generated so that it
   * can reflect any changes in the game setup
   * @param setup The setup to use to mirror the UI
   */
  public void setSetup(GameSetup setup)
  {
    m_setup = setup;
  }

  public void propertyChange(PropertyChangeEvent e)
  {
    if (e.getSource().getClass() == JProgressBar.class)
    {
      JProgressBar prog = (JProgressBar)e.getSource();
      if (e.getPropertyName() == "max")
      {
        prog.setMaximum((Integer)e.getNewValue());
      }
      if (e.getPropertyName() == "val")
      {
        prog.setValue((Integer)e.getNewValue());
      }
      if (e.getPropertyName() == "str")
      {
        prog.setString((String)e.getNewValue());
      }
      logger.fine("Property Change: ProgressBar[" + e.getPropertyName() + "] from " + e.getOldValue() + " to " + e.getNewValue());
    }
  }

  public void setWarningBorder(boolean good)
  {
    if (good)
      m_maincontent.setBorder(new LineBorder(Color.BLACK, 10));
    else
      m_maincontent.setBorder(new LineBorder(Color.RED, 10));
  }

}
