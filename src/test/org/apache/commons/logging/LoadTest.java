/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//logging/src/test/org/apache/commons/logging/LoadTest.java,v 1.2 2003/07/18 14:11:45 rsitze Exp $
 * $Revision: 1.2 $
 * $Date: 2003/07/18 14:11:45 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.commons.logging;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * testcase to emulate container and application isolated from container
 * @author  baliuka
 * @version $Id: LoadTest.java,v 1.2 2003/07/18 14:11:45 rsitze Exp $
 */
public class LoadTest extends TestCase{
    //TODO: need some way to add service provider packages
    static private String LOG_PCKG[] = {"org.apache.commons.logging",
                                        "org.apache.commons.logging.impl"};
    
    static class AppClassLoader extends ClassLoader{
        
        java.util.Map classes = new java.util.HashMap();
        
        AppClassLoader(ClassLoader parent){
            super(parent);
        }
        
        private Class def(String name)throws ClassNotFoundException{
            
            Class result = (Class)classes.get(name);
            if(result != null){
                return result;
            }
            
            try{
                
                java.io.InputStream is = this.getClass().getClassLoader().
                getResourceAsStream( name.replace('.','\\') + ".class" );
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                
                while(is.available() > 0){
                    out.write(is.read());
                }
                
                byte data [] = out.toByteArray();
                
                result = super.defineClass(name, data, 0, data.length );
                classes.put(name,result);
                
                return result;
                
            }catch(java.io.IOException ioe){
                
                throw new ClassNotFoundException( name + " caused by "
                + ioe.getMessage() );
            }
            
            
        }
        
        // not very trivial to emulate we must implement "findClass",
        // but it will delegete to junit class loder first
        public Class loadClass(String name)throws ClassNotFoundException{
            
            //isolates all logging classes, application in the same classloader too.
            //filters exeptions to simlify handling in test
            for(int i = 0; i < LOG_PCKG.length; i++ ){
                if( name.startsWith( LOG_PCKG[i] ) &&
                name.indexOf("Exception") == -1   ){
                    return def(name);
                }
            }
            return super.loadClass(name);
        }
        
    }
    
    
    
    public void testInContainer()throws Exception{
        
        //problem can be in this step (broken app container or missconfiguration)
        //1.  Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        //2.  Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        // we expect this :
        // 1. Thread.currentThread().setContextClassLoader(appLoader);
        // 2. Thread.currentThread().setContextClassLoader(null);
        
        Class cls = reload();
        Thread.currentThread().setContextClassLoader(cls.getClassLoader());
        execute(cls);
        
        cls = reload();
        Thread.currentThread().setContextClassLoader(null);
        execute(cls);
        
        
        cls = reload();
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        try{
            execute(cls);
            fail("SystemClassLoader");
        }catch( LogConfigurationException ok ){
            
        }
        
        
        cls = reload();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try{
            execute(cls);
            fail("ContainerClassLoader");
        }catch( LogConfigurationException ok ){
            
        }
        
    }
    
    private Class reload()throws Exception{
        
        Class testObjCls = null;
        
        AppClassLoader appLoader = new AppClassLoader( this.getClass().
        getClassLoader()
        
        );
        try{
            
            testObjCls = appLoader.loadClass(UserClass.class.getName());
            
        }catch(ClassNotFoundException cnfe){
            throw cnfe;
        }catch(Throwable t){
            t.printStackTrace();
            fail("AppClassLoader failed ");
        }
        
        assertTrue( "app isolated" ,testObjCls.getClassLoader() == appLoader );
        
        
        return testObjCls;
        
        
    }
    
    
    private void execute(Class cls)throws Exception{
            
            cls.newInstance();
        
    }
    
    
    
    /** Creates a new instance of LoadTest */
    public LoadTest(String testName) {
        super(testName);
    }
    
    
    
    
    public static void main(String[] args){
        String[] testCaseName = { LoadTest.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        
        suite.addTestSuite(LoadTest.class);
        
        return suite;
    }
    
}
