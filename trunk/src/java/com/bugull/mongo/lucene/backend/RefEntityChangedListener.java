/**
 * Copyright (c) www.bugull.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bugull.mongo.lucene.backend;

import com.bugull.mongo.BuguDao;
import com.bugull.mongo.BuguEntity;
import com.bugull.mongo.annotations.Ref;
import com.bugull.mongo.annotations.RefList;
import com.bugull.mongo.cache.DaoCache;
import com.bugull.mongo.cache.FieldsCache;
import com.bugull.mongo.lucene.BuguIndex;
import com.bugull.mongo.lucene.annotations.IndexRef;
import com.bugull.mongo.lucene.annotations.IndexRefList;
import com.bugull.mongo.mapper.MapperUtil;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author Frank Wen(xbwen@hotmail.com)
 */
public class RefEntityChangedListener {
    
    private ExecutorService executor = BuguIndex.getInstance().getExecutor();
    
    private Set<Class<?>> refBySet;
    
    public RefEntityChangedListener(Set<Class<?>> refBySet){
        this.refBySet = refBySet;
    }
    
    public void entityChange(Class<?> refClass, String id){
        for(Class<?> cls : refBySet){
            Field[] fields = FieldsCache.getInstance().get(cls);
            for(Field f : fields){
                boolean match = false;
                String fieldName = f.getName();
                Ref ref = f.getAnnotation(Ref.class);
                if(ref!=null && f.getType().equals(refClass) && f.getAnnotation(IndexRef.class)!=null){
                    match = true;
                    String name = ref.name();
                    if(!name.equals("")){
                        fieldName = name;
                    }
                }
                else{
                    RefList refList = f.getAnnotation(RefList.class);
                    if(refList!=null && f.getAnnotation(IndexRefList.class)!=null){
                        ParameterizedType type = (ParameterizedType)f.getGenericType();
                        Type[] types = type.getActualTypeArguments();
                        if(types.length == 1){
                            Class c = (Class)types[0];
                            if(c.equals(refClass)){
                                match = true;
                                String name = refList.name();
                                if(!name.equals("")){
                                    fieldName = name;
                                }
                            }
                        }
                    }
                }
                if(match){
                    BuguDao dao = DaoCache.getInstance().get(cls);
                    List<BuguEntity> list = dao.find(fieldName, MapperUtil.toDBRef(refClass, id));
                    for(BuguEntity o : list){
                        IndexUpdateTask task = new IndexUpdateTask(o);
                        executor.execute(task);
                    }
                }
            }
        }
    }
    
}