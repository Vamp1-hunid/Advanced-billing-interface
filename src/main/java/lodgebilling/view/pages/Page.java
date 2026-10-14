package lodgebilling.view.pages;

import javafx.scene.Node;

/** One screen reachable from the sidebar. */
public interface Page {

    /** The node placed in the centre of the window. */
    Node node();

    /** Re-reads data from the database; called every time the page is shown. */
    void refresh();
}
