package com.kaibla.hamster.ui.test;

import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.base.HamsterSession;
import com.kaibla.hamster.base.UIEngine;

/**
 *
 * @author korend
 */
 public class TestPage extends HamsterPage {

        public TestPage(UIEngine engine, HamsterSession session) {
            super(engine, session);
        }

        public TestPage() {
        }

        @Override
        public String getRefreshLink() {
            return "";
        }

        @Override
        public String getInitialHTMLCode() {
            StringBuilder s = new StringBuilder();
            for (HamsterComponent comp : components) {
                s.append(comp.getHTMLCode());
            }
            htmlCode = s.toString();
            return htmlCode;
        }

        @Override
        public void generateHTMLCode() {
            StringBuilder s = new StringBuilder();
            for (HamsterComponent comp : components) {
                s.append(comp.getHTMLCode());
            }
            htmlCode = s.toString();            
        }

    }