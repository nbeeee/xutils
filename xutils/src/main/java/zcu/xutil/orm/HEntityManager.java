/*
 * Copyright 2009 zaichu xiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zcu.xutil.orm;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

import org.hibernate.Criteria;
import org.hibernate.ObjectDeletedException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;

import zcu.xutil.Disposable;
import zcu.xutil.utils.Function;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class HEntityManager {
	private final SessionFactory factory;
	private XutilSessionContext xsc;

	public HEntityManager(SessionFactory sessionFactory) {
		this.factory = sessionFactory;
	}

	/**
	 * Use open session.
	 *
	 * <pre>
	 * 		Disposable closer = opensession();
	 * 		try{
	 * 			do hibernate code
	 * 		}finally{
	 * 			closer.destroy();
	 * 		}
	 * </pre>
	 *
	 * @return the open session closer
	 */
	public Disposable opensession() {
		if (xsc == null)
			xsc = XutilSessionContext.getSessionContext(factory);
		return xsc.opensession();
	}

	public <T> T execute(Callable<T> callInOpenSession) throws Exception {
		Disposable sessionCloser = opensession();
		try {
			return callInOpenSession.call();
		} finally {
			sessionCloser.destroy();
		}
	}

	public <T> T execute(Function<Session, T> function) {
		Disposable sessionCloser = opensession();
		try {
			return function.apply(factory.getCurrentSession());
		} finally {
			sessionCloser.destroy();
		}
	}

	public Session getCurrentSession() {
		return factory.getCurrentSession();
	}

	public <T> T getReference(Class<T> entityClass, Serializable primaryKey) {
		Disposable sessionCloser = opensession();
		try {
			return entityClass.cast(getCurrentSession().load(entityClass, primaryKey));
		} finally {
			sessionCloser.destroy();
		}
	}

	public <T> T find(Class<T> entityClass, Serializable primaryKey) {
		Disposable sessionCloser = opensession();
		try {
			return entityClass.cast(getCurrentSession().get(entityClass, primaryKey));
		} catch (ObjectDeletedException e) {
			return null;
		} finally {
			sessionCloser.destroy();
		}
	}

	public void persist(Object entity) {
		Disposable sessionCloser = opensession();
		try {
			getCurrentSession().persist(entity);
		} finally {
			sessionCloser.destroy();
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T merge(T entity) {
		Disposable sessionCloser = opensession();
		try {
			return (T) getCurrentSession().merge(entity);
		} finally {
			sessionCloser.destroy();
		}
	}

	public void remove(Object entity) {
		Disposable sessionCloser = opensession();
		try {
			getCurrentSession().delete(entity);
		} finally {
			sessionCloser.destroy();
		}
	}

	public void saveOrUpdate(Object entity) {
		Disposable sessionCloser = opensession();
		try {
			getCurrentSession().saveOrUpdate(entity);
		} finally {
			sessionCloser.destroy();
		}
	}

	public int bulkUpdate(String queryString, Object... values) {
		Disposable sessionCloser = opensession();
		try {
			Query queryObject = getCurrentSession().createQuery(queryString);
			for (int i = values.length - 1; i >= 0; --i)
				queryObject.setParameter(i, values[i]);
			return queryObject.executeUpdate();
		} finally {
			sessionCloser.destroy();
		}
	}

	public List loadAll(Class entityClass, int maxResults) {
		Disposable sessionCloser = opensession();
		try {
			Criteria criteria = getCurrentSession().createCriteria(entityClass);
			if (maxResults > 0)
				criteria.setMaxResults(maxResults);
			return criteria.list();
		} finally {
			sessionCloser.destroy();
		}
	}

	public List find(String queryString, Object... values) {
		Disposable sessionCloser = opensession();
		try {
			Query queryObject = getCurrentSession().createQuery(queryString);
			for (int i = values.length - 1; i >= 0; --i)
				queryObject.setParameter(i, values[i]);
			return queryObject.list();
		} finally {
			sessionCloser.destroy();
		}
	}

	public List findByCriteria(DetachedCriteria criteria, int firstResult, int maxResults) {
		Disposable sessionCloser = opensession();
		try {
			Criteria executableCriteria = criteria.getExecutableCriteria(getCurrentSession());
			if (firstResult >= 0)
				executableCriteria.setFirstResult(firstResult);
			if (maxResults > 0)
				executableCriteria.setMaxResults(maxResults);
			return executableCriteria.list();
		} finally {
			sessionCloser.destroy();
		}
	}
}
