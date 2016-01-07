/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */
package com.kaibla.hamster.testutils;


import com.kaibla.hamster.base.AbstractListenerContainer;
import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.HamsterEngine;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class BaseTest {
    
    public HamsterEngine testEngine;
  
    public HamsterEngine createTestEngine() {    
         if(testEngine !=null) {
            testEngine.destroy();
        }
        HamsterEngine engine = new HamsterEngine() {
            @Override
            public void updateListenerContainer(AbstractListenerContainer listenerContainer) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }            
        };     
        testEngine=engine;
        return engine;
    }
    
    public AbstractListenerContainer createTestListenerContainer(HamsterEngine engine) {
             //we have to set the container into the context
        //otherwise the engine would execute the event handling 
        //asynchronously and not inside our own thread       
        AbstractListenerContainer container= new AbstractListenerContainer(engine, null) {
            
            @Override
            public void dataChanged(DataEvent e) {
            }
            
            @Override
            public boolean isDestroyed() {
                return false;
            }
        };
        Context.setListenerContainer(container);
        return container;
    }

    private static final Logger LOG = getLogger(BaseTest.class.getName());
}
