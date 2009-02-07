/**
 * @file TeamGrid.java by Kyle Lemons, created Jan 15, 2009
 */
package net.kylelemons.halo3;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author eko
 * 
 */
public class TeamGrid extends JPanel
{
  private static final long serialVersionUID = 982006532532745466L;
  public static String[]    TeamNames        = { "Red", // Red : lightPink, darkRed
      "Blue", // Blue : lightBlue, darkBlue
      "Green", // Green : lightGreen, darkGreen
      "Brown", // Brown : Wheat, SaddleBrown
      "Pink", // Pink : lightPink, DeepPink
      "Gold", // Gold : lightYellow, darkGold
      "Purple", // Purple : Thistle, Purple
      "Orange", // Orange : Moccasin, darkOrange
                                             };
  public static Color[]     TeamColors       = { new Color(0xFF, 0xB6, 0xC1), // Red : lightPink, darkRed
      new Color(0xAD, 0xD8, 0xE6), // Blue : lightBlue, darkBlue
      new Color(0x90, 0xEE, 0x90), // Green : lightGreen, darkGreen
      new Color(0xF5, 0xDE, 0xB3), // Brown : Wheat, SaddleBrown
      new Color(0xFF, 0xB6, 0xC1), // Pink : lightPink, DeepPink
      new Color(0xFF, 0xEF, 0xE0), // Gold : lightYellow, darkGold
      new Color(0xD8, 0xBF, 0xD8), // Purple : Thistle, Purple
      new Color(0xFF, 0xE4, 0xB5), // Orange : Moccasin, darkOrange
                                             };
  public static Color[]     TeamText         = { new Color(0x8B, 0x00, 0x00), // Red : lightPink, darkRed
      new Color(0x00, 0x00, 0x8B), // Blue : lightBlue, darkBlue
      new Color(0x00, 0x44, 0x00), // Green : lightGreen, darkGreen
      new Color(0x8B, 0x45, 0x13), // Brown : Wheat, SaddleBrown
      new Color(0xFF, 0x14, 0x93), // Pink : lightPink, DeepPink
      new Color(0xBB, 0x86, 0x0B), // Gold : lightYellow, darkGold
      new Color(0xA0, 0x20, 0xF0), // Purple : Thistle, Purple
      new Color(0xFF, 0x8C, 0x00), // Orange : Moccasin, darkOrange
                                             };

  public boolean            m_showscore;

  /*
   * Colors: "Red", "Orange", "Gold", "Green", "Blue", "Purple", "Brown", "Pink"
   * 
   * CSS Colors: $dark = "dark".$color; $light = "light".$color; if ($light == "lightRed") $light = "lightPink"; if
   * ($light == "lightGold") $light = "lightYellow"; if ($light == "lightOrange") $light = "Moccasin"; if ($light ==
   * "lightPurple") $light = "Thistle"; if ($light == "lightBrown") $light = "Wheat"; if ($dark == "darkGold") $dark =
   * "Gold"; if ($dark == "darkPurple") $dark = "Purple"; if ($dark == "darkBrown") $dark = "SaddleBrown"; if ($dark ==
   * "darkPink") $dark = "DeepPink";
   */

  private ArrayList<Team>   m_teams;
  private GridBagLayout     m_layout;

  public TeamGrid()
  {
    m_teams = new ArrayList<Team>();
    m_layout = new GridBagLayout();
    m_showscore = false;
  }

  public void addTeam(Team t)
  {
    m_teams.add(t);
  }

  private JLabel createColorLabel(String name, Color fgcolor, Color bgcolor, boolean inverted)
  {
    // Use these!
    // int screenWidth = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    int screenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    JLabel label = new JLabel();
    label.setText(name);
    if (inverted)
    {
      label.setForeground(bgcolor);
      label.setBackground(fgcolor);
    }
    else
    {
      label.setForeground(fgcolor);
      label.setBackground(bgcolor);
    }
    label.setOpaque(true);
    label.setHorizontalAlignment(JLabel.CENTER);
    // label.setMinimumSize(new Dimension(400, 400));
    // label.setBorder(new LineBorder(fgcolor, 1));
    if (name.length() <= 12)
      label.setFont(new Font("Serif", Font.BOLD, screenHeight / 38));
    else
      label.setFont(new Font("Serif", Font.BOLD, screenHeight / 42));
    return label;
  }

  /**
   * Any changes to the team lists will be applied
   */
  public void apply()
  {
    int max = 0;
    for (int i = 0; i < m_teams.size(); ++i)
    {
      int sz = m_teams.get(i).getMembers().size();
      if (sz > max) max = sz;
    }
    new Dimension(400, 60);
    m_layout = new GridBagLayout();
    GridBagConstraints constraints;
    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.CENTER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1;
    constraints.weighty = 1;
    constraints.insets.set(2, 2, 2, 2);
    this.setLayout(m_layout);
    this.setMinimumSize(new Dimension(400, 400));
    this.removeAll();
    for (int t = 0; t < m_teams.size(); ++t)
    {
      constraints.gridx = 0;
      constraints.gridy = t;
      constraints.weightx = 0.7;
      ArrayList<String> team = m_teams.get(t).getMembers();
      String name = m_teams.get(t).getTeamName();
      if (name == null || name.length() <= 0) name = TeamNames[t];
      this.add(createColorLabel(name, TeamText[t], TeamColors[t], true), constraints);
      constraints.weightx = 1;
      for (int p = 0; p < max; ++p)
      {
        constraints.gridx = 1 + p;
        if (p < team.size())
          this.add(createColorLabel(team.get(p), TeamText[t], TeamColors[t], false), constraints);
        else
          this.add(createColorLabel("", TeamText[t], TeamColors[t], false), constraints);
      } // add all of the players

      /* This has been removed so as not to cause drama ^_^ */constraints.weightx = 0.2;
      constraints.gridx = max + 1;

      if (m_showscore)
      {
        if (m_teams.get(t).totalSkill() >= 10)
          this.add(createColorLabel("" + m_teams.get(t).totalSkill(), Color.BLACK, TeamColors[t], true), constraints);
      }
      //
    } // add all of the teams
    this.revalidate();
  } // apply

  void clear()
  {
    this.removeAll();
    m_teams.clear();
    this.invalidate();
    this.validate();
  }

  public boolean getShowSkills()
  {
    return m_showscore;
  }

  public void setShowSkills(boolean b)
  {
    m_showscore = b;
  }
}
