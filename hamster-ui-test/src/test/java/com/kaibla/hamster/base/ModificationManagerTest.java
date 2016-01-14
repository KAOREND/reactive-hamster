/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */
package com.kaibla.hamster.base;

import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.base.ModificationManager;
import com.kaibla.hamster.base.DataModel;
import static com.kaibla.hamster.base.UIContext.setPage;
import com.kaibla.hamster.components.container.Container;
import com.kaibla.hamster.components.defaultcomponent.DefaultComponent;
import com.kaibla.hamster.components.list.List;
import com.kaibla.hamster.components.pageswitch.ComponentFactory;
import com.kaibla.hamster.components.pageswitch.PageSwitch;
import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.IntegerAttribute;
import com.kaibla.hamster.persistence.attribute.StringAttribute;
import com.kaibla.hamster.ui.test.UITest;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author kai
 */
public class ModificationManagerTest extends UITest {

    public ModificationManagerTest() {
    }

    /**
     * Test of isStaticRequest method, of class ModificationManager.
     */
    @Test
    public void testVisibilityCheck() {
//        for (int i = 0; i < 100; i++) {
            HamsterPage page = createTestPage(testUIEngine);
            ModificationManager m = page.modManager;
            setPage(page);
            DefaultComponent d = new DefaultComponent(page);
            page.addComponent(d);

            Container container = new Container(page);
            final DefaultComponent page1 = new DefaultComponent(page).addElement("page1");
            final DefaultComponent hidden = new DefaultComponent(page).addElement("hidden");
            final DefaultComponent page2 = new DefaultComponent(page).addElement("page2");
            page1.addElement(hidden);
            page1.setOneSlot(true);
            PageSwitch pageSwitch = new PageSwitch(page, container, "testSwitch");
            loadComponent(page, container, page1, page2, pageSwitch);
            
            pageSwitch.addSwitch("page1", "page1").
                    setComponentFactory(new ComponentFactory(pageSwitch) {

                        @Override
                        public HamsterComponent createComponent() {
                            return page1;
                        }
                    });

            pageSwitch.addSwitch("page2", "page2").
                    setComponentFactory(new ComponentFactory(pageSwitch) {

                        @Override
                        public HamsterComponent createComponent() {
                            return page2;
                        }
                    });
           
            d.addElement(pageSwitch);
            d.addElement(container);
            pageSwitch.activateSwitch("page1");
            String t = m.getModificiationXML();
            assertTrue("page should be visible now", page.isVisible());
            assertTrue("d should be visible now", d.isVisible());
            assertTrue("pageSwitch should be visible now", pageSwitch.isVisible());
            assertTrue("page1 should be visible now", page1.isVisible());
            assertFalse("page2 should not be visible now", page2.isVisible());
            assertFalse("page2 should not be visible now", t.contains("" + page2.getId()));
            m.confirmLastModificationXML(m.getConfirmationCounter());
            pageSwitch.activateSwitch("page2");
            t = m.getModificiationXML();
            assertTrue("page should be visible now", page.isVisible());
            assertTrue("pageSwitch should be visible now", pageSwitch.isVisible());
            assertFalse("page1 should be hidden now", page1.isVisible());
            assertFalse("page1 should not be visible now \n" + t, t.contains("" + page1.getId()));
            assertTrue("page2 should  be visible now", page2.isVisible());
            assertTrue("page2 should  be visible now", t.contains("" + page2.getId()));

            m.confirmLastModificationXML(m.getConfirmationCounter());
            //check invisible updates
            hidden.markForUpdate();
            t = m.getModificiationXML();

            assertFalse("page1 should not be rendered now", t.contains("page1"));
            assertFalse("hidden should not be rendered now", t.contains("hidden"));
            m.confirmLastModificationXML(m.getConfirmationCounter() + 2);
            pageSwitch.activateSwitch("page1");
            t = m.getModificiationXML();
            assertTrue("page1 should  be rendered now", t.contains("page1"));
            assertTrue("hidden should  be rendered now", t.contains("hidden"));
            System.out.println(t);

            m.confirmLastModificationXML(m.getConfirmationCounter() + 2);
            t = m.getModificiationXML();
            assertFalse("page1 should not be rendered now", t.contains("page1"));
            assertFalse("hidden should not be rendered now", t.contains("hidden"));
//        }
    }

    private boolean onlyAllowAppends = false;

    public boolean isOnlyAllowAppends() {
        return onlyAllowAppends;
    }

    @Test
    public void testListAppend() {
        onlyAllowAppends = false;
        final HamsterPage page = createTestPage(testUIEngine);
        ModificationManager m = new ModificationManager(page) {
            @Override
            public synchronized void addMod(HamsterComponent comp) {
                if (isOnlyAllowAppends()) {
                    fail("addMod is not allowed in this case");
                }
                super.addMod(comp);
            }
        };
        page.modManager = m;
        setPage(page);
        DefaultComponent d = new DefaultComponent(page);
        page.addComponent(d);

        final StringAttribute testAttribute = new StringAttribute(testTable.getClass(), "test");
        final IntegerAttribute orderAttribute = new IntegerAttribute(testTable.getClass(), "test");

        for (int i = 0; i < 2; i++) {
            Document t = testTable.createNew();
            t.set(testAttribute, "hello" + i);
            t.set(orderAttribute, i);
            t.writeToDatabase();
        }
        List l = new List(page, "testList", testTable.query(d, new Query().addSortCriteria(orderAttribute, false))) {
            @Override
            public HamsterComponent renderElement(DataModel data) {
                DefaultComponent e = new DefaultComponent(page);
                Document o = (Document) data;
                e.acquireDataModel(data);
                e.addElement(o.getStringSource(testAttribute));
                return e;
            }

        };
        d.addElement(l);
        loadComponent(page, d);
        loadComponentClass(List.class);
        LOG.log(Level.INFO, "mode XML: \n+{0}", m.getModificiationXML());
        m.reset();
        assertTrue("there should be no modifications other than appends, but there have been " + m.notConfirmed.
                size(), m.notConfirmed.isEmpty());
        onlyAllowAppends = false;
        Document newEntry = testTable.createNew();
        newEntry.set(testAttribute, "helloNewEntry");
        newEntry.set(orderAttribute, 15);
        newEntry.writeToDatabase();

        LOG.log(Level.INFO, "mode XML: \n+{0}", m.getModificiationXML());
        assertTrue("there should be no modifications other than appends, but there have been " + m.notConfirmed.
                size(), m.notConfirmed.isEmpty());
        assertTrue("there should only be one append, but there have been " + m.notConfirmedAppends.
                size(), m.notConfirmedAppends.size() == 1);
    }
    private static final Logger LOG = getLogger(ModificationManagerTest.class.getName());

}
