/*
Copyright (C) 2003 JabRef team

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref;

import java.util.Hashtable;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import net.sf.jabref.search.*;
import net.sf.jabref.search.SearchExpression;

class SearchManager2 extends SidePaneComponent
    implements ActionListener, KeyListener, ItemListener, CaretListener, ErrorMessageDisplay {

    private JabRefFrame frame;

    GridBagLayout gbl = new GridBagLayout() ;
    GridBagConstraints con = new GridBagConstraints() ;

    IncrementalSearcher incSearcher;

    //private JabRefFrame frame;
    private JTextField searchField = new JTextField("", 12);
    private JLabel lab = //new JLabel(Globals.lang("Search")+":");
	new JLabel(new ImageIcon(GUIGlobals.searchIconFile));
    private JPopupMenu settings = new JPopupMenu();
    private JButton openset = new JButton(Globals.lang("Settings"));
    private JButton escape = new JButton(Globals.lang("Clear"));
    private JButton help = new JButton(new ImageIcon(GUIGlobals.helpIconFile));
    /** This button's text will be set later. */
    private JButton search = new JButton();
    private JCheckBoxMenuItem searchReq, searchOpt, searchGen,
	searchAll, caseSensitive, regExpSearch;

    private JRadioButton increment, highlight, reorder;
    private JCheckBoxMenuItem select;
    private ButtonGroup types = new ButtonGroup();
    private boolean incSearch = false;

    private int incSearchPos = -1; // To keep track of where we are in
				   // an incremental search. -1 means
				   // that the search is inactive.


    public SearchManager2(JabRefFrame frame, SidePaneManager manager) {
	super(manager, GUIGlobals.searchIconFile, Globals.lang("Search"));

        this.frame = frame;
	incSearcher = new IncrementalSearcher(Globals.prefs);


	
	//setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.magenta));

        searchReq = new JCheckBoxMenuItem
	    (Globals.lang("Search required fields"),
	     Globals.prefs.getBoolean("searchReq"));
	searchOpt = new JCheckBoxMenuItem
	    (Globals.lang("Search optional fields"),
	     Globals.prefs.getBoolean("searchOpt"));
	searchGen = new JCheckBoxMenuItem
	    (Globals.lang("Search general fields"),
	     Globals.prefs.getBoolean("searchGen"));
        searchAll = new JCheckBoxMenuItem
	    (Globals.lang("Search all fields"),
	     Globals.prefs.getBoolean("searchAll"));
        regExpSearch = new JCheckBoxMenuItem
	    (Globals.lang("Use regular expressions"),
	     Globals.prefs.getBoolean("regExpSearch"));

	
	increment = new JRadioButton(Globals.lang("Incremental"), false);
	highlight = new JRadioButton(Globals.lang("Highlight"), true);
	reorder = new JRadioButton(Globals.lang("Float"), false);
	types.add(increment);
	types.add(highlight);
	types.add(reorder);


        select = new JCheckBoxMenuItem(Globals.lang("Select matches"), false);
        increment.setToolTipText(Globals.lang("Incremental search"));
        highlight.setToolTipText(Globals.lang("Gray out non-matching entries"));
        reorder.setToolTipText(Globals.lang("Move matching entries to the top"));

	// Add an item listener that makes sure we only listen for key events
	// when incremental search is turned on.
	increment.addItemListener(this);
	reorder.addItemListener(this);

        // Add the global focus listener, so a menu item can see if this field was focused when
        // an action was called.
        searchField.addFocusListener(Globals.focusListener);


	if (searchAll.isSelected()) {
	    searchReq.setEnabled(false);
	    searchOpt.setEnabled(false);
	    searchGen.setEnabled(false);
	}
    searchAll.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent event) {
            boolean state = !searchAll.isSelected();
            searchReq.setEnabled(state);
	        searchOpt.setEnabled(state);
	        searchGen.setEnabled(state);            
        }
    });

        caseSensitive = new JCheckBoxMenuItem(Globals.lang("Case sensitive"),
				      Globals.prefs.getBoolean("caseSensitiveSearch"));
settings.add(select);

    // 2005.03.29, trying to remove field category searches, to simplify
        // search usability.
    //settings.addSeparator();
	//settings.add(searchReq);
	//settings.add(searchOpt);
	//settings.add(searchGen);
	//settings.addSeparator();
	//settings.add(searchAll);
    // ---------------------------------------------------------------
	settings.addSeparator();
        settings.add(caseSensitive);
	settings.add(regExpSearch);
	//settings.addSeparator();


	searchField.addActionListener(this);
    searchField.addCaretListener(this);
        search.addActionListener(this);
	searchField.addFocusListener(new FocusAdapter() {
          public void focusGained(FocusEvent e) {
            if (increment.isSelected())
              searchField.setText("");
          }
		public void focusLost(FocusEvent e) {
		    incSearch = false;
		    incSearchPos = -1; // Reset incremental
				       // search. This makes the
				       // incremental search reset
				       // once the user moves focus to
				       // somewhere else.
                    if (increment.isSelected()) {
                      //searchField.setText("");
                      //System.out.println("focuslistener");
                    }
		}
	    });
	escape.addActionListener(this);

	openset.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                  if (settings.isVisible()) {
                    //System.out.println("oee");
                    //settings.setVisible(false);
                  }
                  else {
                    JButton src = (JButton) e.getSource();
                    settings.show(src, 0, openset.getHeight());
                  }
		}
	    });

            Insets margin = new Insets(0, 2, 0, 2);
            //search.setMargin(margin);
            escape.setMargin(margin);
            openset.setMargin(margin);
            Dimension butDim = new Dimension(20, 20);
            help.setPreferredSize(butDim);
            help.setMinimumSize(butDim);
            help.setMargin(margin);
            help.addActionListener(new HelpAction(Globals.helpDiag, GUIGlobals.searchHelp, "Help"));

	if (Globals.prefs.getBoolean("incrementS"))
	    increment.setSelected(true);
	else if (!Globals.prefs.getBoolean("selectS"))
	    reorder.setSelected(true);

        JPanel main = new JPanel();
	main.setLayout(gbl);
	//SidePaneHeader header = new SidePaneHeader("Search", GUIGlobals.searchIconFile, this);
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.fill = GridBagConstraints.BOTH;
        con.weightx = 1;
	//con.insets = new Insets(0, 0, 2,  0);
	//gbl.setConstraints(header, con);
	//add(header);
        //con.insets = new Insets(0, 0, 0,  0);
        gbl.setConstraints(searchField,con);
        main.add(searchField) ;
        //con.gridwidth = 1;
        gbl.setConstraints(search,con);
        main.add(search) ;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(escape,con);
        main.add(escape) ;
        con.insets = new Insets(0, 2, 0,  0);
	gbl.setConstraints(increment, con);
        main.add(increment);
	gbl.setConstraints(highlight, con);
        main.add(highlight);
	gbl.setConstraints(reorder, con);
        main.add(reorder);
        con.insets = new Insets(0, 0, 0,  0);
        JPanel pan = new JPanel();
        GridBagLayout gb = new GridBagLayout();
        gbl.setConstraints(pan, con);
        pan.setLayout(gb);
        con.weightx = 1;
        con.gridwidth = 1;
        gb.setConstraints(openset, con);
        pan.add(openset);
        con.weightx = 0;
        gb.setConstraints(help, con);
        pan.add(help);
        main.add(pan);
        main.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        add(main, BorderLayout.CENTER);

	searchField.getInputMap().put(Globals.prefs.getKey("Repeat incremental search"),
				      "repeat");

	searchField.getActionMap().put("repeat", new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    if (increment.isSelected())
			repeatIncremental();
		}
	    });
	searchField.getInputMap().put(Globals.prefs.getKey("Clear search"), "escape");
	searchField.getActionMap().put("escape", new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
            hideAway();
		    //SearchManager2.this.actionPerformed(new ActionEvent(escape, 0, ""));
		}
	    });
    setSearchButtonSizes();
    updateSearchButtonText();
    }

    /** force the search button to be large enough for
     * the longer of the two texts */
    private void setSearchButtonSizes() {
        search.setText(Globals.lang("Search Specified Field(s)"));
        Dimension size1 = search.getPreferredSize();
        search.setText(Globals.lang("Search All Fields"));
        Dimension size2 = search.getPreferredSize();
        size2.width = Math.max(size1.width,size2.width); 
        search.setMinimumSize(size2);
        search.setPreferredSize(size2);
    }

    public void updatePrefs() {
	Globals.prefs.putBoolean("searchReq", searchReq.isSelected());
	Globals.prefs.putBoolean("searchOpt", searchOpt.isSelected());
	Globals.prefs.putBoolean("searchGen", searchGen.isSelected());
	Globals.prefs.putBoolean("searchAll", searchAll.isSelected());
	Globals.prefs.putBoolean("incrementS", increment.isSelected());
	Globals.prefs.putBoolean("selectS", highlight.isSelected());
	//	Globals.prefs.putBoolean("grayOutNonHits", grayOut.isSelected());
	Globals.prefs.putBoolean("caseSensitiveSearch",
			 caseSensitive.isSelected());
	Globals.prefs.putBoolean("regExpSearch", regExpSearch.isSelected());

    }

    public void startIncrementalSearch() {
	increment.setSelected(true);
	searchField.setText("");
        //System.out.println("startIncrementalSearch");
	searchField.requestFocus();
    }

    /**
     * Clears and focuses the search field if it is not
     * focused. Otherwise, cycles to the next search type.
     */
    public void startSearch() {
	if (increment.isSelected() && incSearch) {
	    repeatIncremental();
	    return;
	}
	if (!searchField.hasFocus()) {
	    //searchField.setText("");
            searchField.selectAll();
	    searchField.requestFocus();
	} else {
	    if (increment.isSelected())
		highlight.setSelected(true);
	    else if (highlight.isSelected())
		reorder.setSelected(true);
	    else {
		increment.setSelected(true);
	    }
	    increment.revalidate();
	    increment.repaint();

        searchField.requestFocus();

	}
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == escape) {
	    incSearch = false;
	    if (panel != null) {
		(new Thread() {
			public void run() {
			    panel.stopShowingSearchResults();
			}
		    }).start();

	    }
	}
	else if (((e.getSource() == searchField) || (e.getSource() == search))
		 && !increment.isSelected()
		 && (panel != null)) {
	    updatePrefs(); // Make sure the user's choices are recorded.
            if (searchField.getText().equals("")) {
              // An empty search field should cause the search to be cleared.
              panel.stopShowingSearchResults();
              return;
            }
	    // Setup search parameters common to both highlight and float.
	    Hashtable searchOptions = new Hashtable();
	    searchOptions.put("option",searchField.getText()) ;
	    SearchRuleSet searchRules = new SearchRuleSet() ;
	    SearchRule rule1;
	    if (Globals.prefs.getBoolean("regExpSearch"))
	        rule1 = new RegExpRule(
                    Globals.prefs.getBoolean("caseSensitiveSearch"));
	    else
	        rule1 = new SimpleSearchRule(
                    Globals.prefs.getBoolean("caseSensitiveSearch"));

		try {
			// this searches specified fields if specified, 
            // and all fields otherwise
			rule1 = new SearchExpression(Globals.prefs,searchOptions);
		} catch (Exception ex) {
            // we'll do a search in all fields
		}
//		} catch (PatternSyntaxException ex) {
//			System.out.println(ex);
//			return;
//		} catch (TokenStreamException ex) {
//			System.out.println(ex);
//			return;
//		} catch (RecognitionException ex) {
//			System.out.println(ex);
//			return;
//		}

	    searchRules.addRule(rule1) ;
        panel.setSearchMatcher(new SearchMatcher(searchRules, searchOptions));
        panel.output(Globals.lang("Searched database. Number of hits")
                    + ": " + panel.mainTable.getRowCount());
        /*
        if (reorder.isSelected()) {
		// Float search.
		DatabaseSearch search = new DatabaseSearch
		    (this, searchOptions,searchRules, panel,
		     Globals.SEARCH, true, true//Globals.Globals.prefs.getBoolean("grayOutNonHits")
                    , select.isSelected());
		search.start() ;
	    }
	    else if (highlight.isSelected()) {
		// Highlight search.
		DatabaseSearch search = new DatabaseSearch
		    (this, searchOptions,searchRules, panel,
		     Globals.SEARCH, false, true, select.isSelected());
		search.start() ;
	    }
        */
	    // Afterwards, select all text in the search field.
	    searchField.select(0,searchField.getText().length()) ;
	    //new FocusRequester(frame.basePanel().entryTable);

	}
    }

    public void itemStateChanged(ItemEvent e) {
	if (e.getSource() == increment) {
        updateSearchButtonText();
	    if (increment.isSelected())
		searchField.addKeyListener(this);
	    else
		searchField.removeKeyListener(this);
    } else if (e.getSource() == highlight) {
        updateSearchButtonText();
	} else if (e.getSource() == reorder) {
        updateSearchButtonText();
	    // If this search type is disabled, remove reordering from
	    // all databases.
	    if (!reorder.isSelected()) {
		panel.stopShowingSearchResults();
	    }
	}
    }

    private void repeatIncremental() {
	incSearchPos++;
	if (panel != null)
	    goIncremental();
    }

    /**
     * Used for incremental search. Only activated when incremental
     * is selected.
     *
     * The variable incSearchPos keeps track of which entry was last
     * checked.
     */
    public void keyTyped(KeyEvent e) {
	if (e.isControlDown()) {
	    return;
	}
	if (panel != null)
	    goIncremental();
    }

    private void goIncremental() {
	incSearch = true;
	SwingUtilities.invokeLater(new Thread() {
		public void run() {
		    String text = searchField.getText();


		    if (incSearchPos >= panel.getDatabase().getEntryCount()) {
			panel.output("'"+text+"' : "+Globals.lang

				     ("Incremental search failed. Repeat to search from top.")+".");
			incSearchPos = -1;
			return;
		    }

		    if (searchField.getText().equals("")) return;
		    if (incSearchPos < 0)
			incSearchPos = 0;
		    BibtexEntry be = panel.mainTable.getEntryAt(incSearchPos);
		    while (!incSearcher.search(text, be)) {
			    incSearchPos++;
			    if (incSearchPos < panel.getDatabase().getEntryCount())
			        be = panel.mainTable.getEntryAt(incSearchPos);
			else {
			    panel.output("'"+text+"' : "+Globals.lang
					 ("Incremental search failed. Repeat to search from top."));
			    incSearchPos = -1;
			    return;
			}
		    }
		    if (incSearchPos >= 0) {

			panel.selectSingleEntry(incSearchPos);
			panel.output("'"+text+"' "+Globals.lang

				     ("found")+".");
                
		    }
		}
	    });
    }

    public void componentClosing() {
	frame.searchToggle.setSelected(false);
	if (panel != null)
	    panel.stopShowingSearchResults();
    }


    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

    public void caretUpdate(CaretEvent e) {
        if (e.getSource() == searchField) {
            updateSearchButtonText();
        }
    }
    
	/** Updates the text on the search button to reflect
      * the type of search that will happen on click. */
    private void updateSearchButtonText() {
        search.setText(!increment.isSelected() 
                && SearchExpressionParser.checkSyntax(
                searchField.getText(),
                caseSensitive.isSelected(),
                regExpSearch.isSelected()) != null 
                ? Globals.lang("Search Specified Field(s)") 
                : Globals.lang("Search All Fields"));
    }

    /**
     * This method is required by the ErrorMessageDisplay interface, and lets this class
     * serve as a callback for regular expression exceptions happening in DatabaseSearch.
     * @param errorMessage
     */
    public void reportError(String errorMessage) {
        JOptionPane.showMessageDialog(panel, errorMessage, Globals.lang("Search error"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * This method is required by the ErrorMessageDisplay interface, and lets this class
     * serve as a callback for regular expression exceptions happening in DatabaseSearch.
     * @param errorMessage
     */
    public void reportError(String errorMessage, Exception exception) {
        reportError(errorMessage);
    }
}
