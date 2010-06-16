/*******************************************************************************
 * Copyright 2010, Topic Map Lab ( http://www.topicmapslab.de )
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.topicmapslab.majortom.inMemory.store.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import de.topicmapslab.majortom.core.ScopeImpl;
import de.topicmapslab.majortom.inMemory.store.model.IDataStore;
import de.topicmapslab.majortom.model.core.IAssociation;
import de.topicmapslab.majortom.model.core.IName;
import de.topicmapslab.majortom.model.core.IOccurrence;
import de.topicmapslab.majortom.model.core.IScopable;
import de.topicmapslab.majortom.model.core.IScope;
import de.topicmapslab.majortom.model.core.ITopic;
import de.topicmapslab.majortom.model.core.IVariant;
import de.topicmapslab.majortom.model.exception.TopicMapStoreException;
import de.topicmapslab.majortom.model.revision.IRevision;
import de.topicmapslab.majortom.util.HashUtil;

/**
 * Internal data store of scoped-scope relations
 * 
 * @author Sven Krosse
 */
public class ScopeStore implements IDataStore {

	/**
	 * storage map of scope-themes mapping
	 */
	private Map<IScope, Set<ITopic>> scopes;
	/**
	 * storage map of scope-name relation
	 */
	private Map<IScope, Set<IName>> scopedNames;
	/**
	 * storage map of scope-occurrence relation
	 */
	private Map<IScope, Set<IOccurrence>> scopedOccurrences;
	/**
	 * storage map of scope-variant relation
	 */
	private Map<IScope, Set<IVariant>> scopedVariants;
	/**
	 * storage map of scope-association relation
	 */
	private Map<IScope, Set<IAssociation>> scopedAssociations;
	/**
	 * storage map of name-scope relation
	 */
	private Map<IName, IScope> nameScopes;
	/**
	 * storage map of occurrence-scope relation
	 */
	private Map<IOccurrence, IScope> occurrenceScopes;
	/**
	 * storage map of variant-scope relation
	 */
	private Map<IVariant, IScope> variantScopes;
	/**
	 * storage map of association-scope relation
	 */
	private Map<IAssociation, IScope> associationScopes;
	/**
	 * empty scope
	 */
	private IScope emptyScope;

	/**
	 * constructor
	 */
	public ScopeStore() {
		emptyScope = new ScopeImpl();
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() {
		if (scopes != null) {
			scopes.clear();
		}
		if (scopedNames != null) {
			scopedNames.clear();
		}
		if (scopedOccurrences != null) {
			scopedOccurrences.clear();
		}
		if (scopedVariants != null) {
			scopedVariants.clear();
		}
		if (scopedAssociations != null) {
			scopedAssociations.clear();
		}
		if (nameScopes != null) {
			nameScopes.clear();
		}
		if (occurrenceScopes != null) {
			occurrenceScopes.clear();
		}
		if (variantScopes != null) {
			variantScopes.clear();
		}
		if (associationScopes != null) {
			associationScopes.clear();
		}
	}

	/**
	 * Returns the scope instance for the given themes
	 * 
	 * @param themes the themes
	 * @return the scope instance
	 */
	public IScope getScope(ITopic... themes) {
		return getScope(Arrays.asList(themes));
	}

	/**
	 * Returns the scope instance for the given themes
	 * 
	 * @param themes the themes
	 * @return the scope instance
	 */
	public IScope getScope(Collection<ITopic> themes) {
		if (themes.isEmpty()) {
			return emptyScope;
		}
		if (scopes == null) {
			scopes = HashUtil.getHashMap();
		}
		for (Entry<IScope, Set<ITopic>> entry : scopes.entrySet()) {
			if (entry.getValue().size() == themes.size() && entry.getValue().containsAll(themes)) {
				return entry.getKey();
			}
		}
		Set<ITopic> set = HashUtil.getHashSet();
		set.addAll(themes);
		IScope scope = new ScopeImpl(set);
		scopes.put(scope, set);
		return scope;
	}

	/**
	 * Returns all scoped items of the given scope.
	 * 
	 * @param scope the scope
	 * @return the scoped construct
	 */
	public Set<IScopable> getScoped(IScope scope) {
		Set<IScopable> scoped = HashUtil.getHashSet();
		scoped.addAll(getScopedAssociations(scope));
		scoped.addAll(getScopedOccurrences(scope));
		scoped.addAll(getScopedNames(scope));
		scoped.addAll(getScopedVariants(scope));
		return scoped;
	}

	/**
	 * Returns all scoped occurrences of the given scope.
	 * 
	 * @param scope the scope
	 * @return the scoped occurrences
	 */
	public Set<IOccurrence> getScopedOccurrences(IScope scope) {
		if (scopedOccurrences != null && scopedOccurrences.containsKey(scope)) {
			return scopedOccurrences.get(scope);
		}
		return HashUtil.getHashSet();
	}

	/**
	 * Returns all scoped name items of the given scope.
	 * 
	 * @param scope the scope
	 * @return the scoped names
	 */
	public Set<IName> getScopedNames(IScope scope) {
		if (scopedNames != null && scopedNames.containsKey(scope)) {
			return scopedNames.get(scope);
		}
		return HashUtil.getHashSet();
	}

	/**
	 * Returns all scoped variant items of the given scope.
	 * 
	 * @param scope the scope
	 * @return the scoped variant items
	 */
	public Set<IVariant> getScopedVariants(IScope scope) {
		Set<IVariant> set = HashUtil.getHashSet();
		if (scopedVariants != null) {
			for (Entry<IVariant, IScope> entry : variantScopes.entrySet()) {
				Set<ITopic> themes = HashUtil.getHashSet(entry.getValue().getThemes());
				themes.addAll(entry.getKey().getParent().getScopeObject().getThemes());
				IScope s = getScope(themes);
				if (scope.equals(s)) {
					set.add(entry.getKey());
				}
			}
		}
		return set;
	}

	/**
	 * Returns all scoped association items of the given scope.
	 * 
	 * @param scope the scope
	 * @return the scoped association items
	 */
	public Set<IAssociation> getScopedAssociations(IScope scope) {
		if (scopedAssociations != null && scopedAssociations.containsKey(scope)) {
			return scopedAssociations.get(scope);
		}
		return HashUtil.getHashSet();
	}

	/**
	 * Returns the scope of the scoped construct
	 * 
	 * @param scoped the scoped construct
	 * @return the scope and never <code>null</code>
	 */
	public IScope getScope(IScopable scoped) {
		if (scoped instanceof IAssociation) {
			return getScope((IAssociation) scoped);
		} else if (scoped instanceof IOccurrence) {
			return getScope((IOccurrence) scoped);
		} else if (scoped instanceof IName) {
			return getScope((IName) scoped);
		} else if (scoped instanceof IVariant) {
			return getScope((IVariant) scoped);
		} else {
			throw new TopicMapStoreException("Type of scoped item is unknown '" + scoped.getClass() + "'.");
		}
	}

	/**
	 * Returns the scope of the scoped construct
	 * 
	 * @param scoped the scoped construct
	 * @return the scope and never <code>null</code>
	 */
	public IScope getScope(IAssociation scoped) {
		if (associationScopes != null && associationScopes.containsKey(scoped)) {
			return associationScopes.get(scoped);
		}
		return emptyScope;
	}

	/**
	 * Returns the scope of the scoped construct
	 * 
	 * @param scoped the scoped construct
	 * @return the scope and never <code>null</code>
	 */
	public IScope getScope(IOccurrence scoped) {
		if (occurrenceScopes != null && occurrenceScopes.containsKey(scoped)) {
			return occurrenceScopes.get(scoped);
		}
		return emptyScope;
	}

	/**
	 * Returns the scope of the scoped construct
	 * 
	 * @param scoped the scoped construct
	 * @return the scope and never <code>null</code>
	 */
	public IScope getScope(IName scoped) {
		if (nameScopes != null && nameScopes.containsKey(scoped)) {
			return nameScopes.get(scoped);
		}
		return emptyScope;
	}

	/**
	 * Returns the scope of the scoped construct
	 * 
	 * @param scoped the scoped construct
	 * @return the scope and never <code>null</code>
	 */
	public IScope getScope(IVariant scoped) {
		Set<ITopic> themes = HashUtil.getHashSet();
		if (variantScopes != null && variantScopes.containsKey(scoped)) {
			themes.addAll(variantScopes.get(scoped).getThemes());
		}
		themes.addAll(getScope(scoped.getParent()).getThemes());
		return getScope(themes);
	}

	/**
	 * Remove the relation between the scoped construct and the stored scope.
	 * 
	 * @param scoped the scoped construct
	 * @return the old scope
	 */
	public IScope removeScope(IScopable scoped) {
		if (scoped instanceof IAssociation) {
			return removeScope((IAssociation) scoped);
		} else if (scoped instanceof IOccurrence) {
			return removeScope((IOccurrence) scoped);
		} else if (scoped instanceof IName) {
			return removeScope((IName) scoped);
		} else if (scoped instanceof IVariant) {
			return removeScope((IVariant) scoped);
		} else {
			throw new TopicMapStoreException("Type of scoped item is unknown '" + scoped.getClass() + "'.");
		}
	}

	/**
	 * Remove the relation between the scoped construct and the stored scope.
	 * 
	 * @param scoped the scoped construct
	 * @return the old scope
	 */
	public IScope removeScope(IAssociation scoped) {
		if (associationScopes != null && associationScopes.containsKey(scoped)) {
			IScope s = associationScopes.remove(scoped);
			Set<IAssociation> set = scopedAssociations.get(s);
			set.remove(scoped);
			if (set.isEmpty()) {
				scopedAssociations.remove(s);
			} else {
				scopedAssociations.put(s, set);
			}
			return s;
		}
		return emptyScope;
	}

	/**
	 * Remove the relation between the scoped construct and the stored scope.
	 * 
	 * @param scoped the scoped construct
	 * @return the old scope
	 */
	public IScope removeScope(IOccurrence scoped) {
		if (occurrenceScopes != null && occurrenceScopes.containsKey(scoped)) {
			IScope s = occurrenceScopes.remove(scoped);
			Set<IOccurrence> set = scopedOccurrences.get(s);
			set.remove(scoped);
			if (set.isEmpty()) {
				scopedOccurrences.remove(s);
			} else {
				scopedOccurrences.put(s, set);
			}
			return s;
		}
		return emptyScope;
	}

	/**
	 * Remove the relation between the scoped construct and the stored scope.
	 * 
	 * @param scoped the scoped construct
	 * @return the old scope
	 */
	public IScope removeScope(IName scoped) {
		if (nameScopes != null && nameScopes.containsKey(scoped)) {
			IScope s = nameScopes.remove(scoped);
			Set<IName> set = scopedNames.get(s);
			set.remove(scoped);
			if (set.isEmpty()) {
				scopedNames.remove(s);
			} else {
				scopedNames.put(s, set);
			}
			return s;
		}
		return emptyScope;
	}

	/**
	 * Remove the relation between the scoped construct and the stored scope.
	 * 
	 * @param scoped the scoped construct
	 * @return the old scope
	 */
	public IScope removeScope(IVariant scoped) {
		if (variantScopes != null && variantScopes.containsKey(scoped)) {
			IScope s = variantScopes.remove(scoped);
			Set<IVariant> set = scopedVariants.get(s);
			set.remove(scoped);
			if (set.isEmpty()) {
				scopedVariants.remove(s);
			} else {
				scopedVariants.put(s, set);
			}
			return s;
		}
		return emptyScope;
	}

	/**
	 * Store the relation between the given scoped item and the scope.
	 * 
	 * @param scoped the scoped construct
	 * @param s the scope
	 */
	public void setScope(IScopable scoped, IScope s) {
		if (scoped instanceof IAssociation) {
			setScope((IAssociation) scoped, s);
		} else if (scoped instanceof IOccurrence) {
			setScope((IOccurrence) scoped, s);
		} else if (scoped instanceof IName) {
			setScope((IName) scoped, s);
		} else if (scoped instanceof IVariant) {
			setScope((IVariant) scoped, s);
		} else {
			throw new TopicMapStoreException("Type of scoped item is unknown '" + scoped.getClass() + "'.");
		}
	}

	/**
	 * Store the relation between the given scoped item and the scope.
	 * 
	 * @param scoped the scoped construct
	 * @param s the scope
	 */
	public void setScope(IAssociation scoped, IScope s) {
		if (associationScopes == null) {
			associationScopes = HashUtil.getHashMap();
		}
		associationScopes.put(scoped, s);
		if (scopedAssociations == null) {
			scopedAssociations = HashUtil.getHashMap();
		}
		Set<IAssociation> set = scopedAssociations.get(s);
		if (set == null) {
			set = HashUtil.getHashSet();
		}
		set.add(scoped);
		scopedAssociations.put(s, set);
	}

	/**
	 * Store the relation between the given scoped item and the scope.
	 * 
	 * @param scoped the scoped construct
	 * @param s the scope
	 */
	public void setScope(IOccurrence scoped, IScope s) {
		if (occurrenceScopes == null) {
			occurrenceScopes = HashUtil.getHashMap();
		}
		occurrenceScopes.put(scoped, s);
		if (scopedOccurrences == null) {
			scopedOccurrences = HashUtil.getHashMap();
		}
		Set<IOccurrence> set = scopedOccurrences.get(s);
		if (set == null) {
			set = HashUtil.getHashSet();
		}
		set.add(scoped);
		scopedOccurrences.put(s, set);
	}

	/**
	 * Store the relation between the given scoped item and the scope.
	 * 
	 * @param scoped the scoped construct
	 * @param s the scope
	 */
	public void setScope(IName scoped, IScope s) {
		if (nameScopes == null) {
			nameScopes = HashUtil.getHashMap();
		}
		nameScopes.put(scoped, s);
		if (scopedNames == null) {
			scopedNames = HashUtil.getHashMap();
		}
		Set<IName> set = scopedNames.get(s);
		if (set == null) {
			set = HashUtil.getHashSet();
		}
		set.add(scoped);
		scopedNames.put(s, set);
	}

	/**
	 * Store the relation between the given scoped item and the scope.
	 * 
	 * @param scoped the scoped construct
	 * @param s the scope
	 */
	public void setScope(IVariant scoped, IScope s) {
		// IScope parent = getScope(scoped.getParent());
		// /*
		// * check if theme is not part of the name scope
		// */
		// if (!s.getThemes().containsAll(parent.getThemes())) {
		// throw new ModelConstraintException(scoped,
		// "The variant scope has to contain all themes of the parent name scope.");
		// }
		//
		// /*
		// * check if theme is not part of the name scope
		// */
		// if (parent.getThemes().size() >= s.getThemes().size()) {
		// throw new ModelConstraintException(scoped,
		// "The variant scope has to contain at least one more theme than the parent name.");
		// }

		if (variantScopes == null) {
			variantScopes = HashUtil.getHashMap();
		}
		variantScopes.put(scoped, s);
		if (scopedVariants == null) {
			scopedVariants = HashUtil.getHashMap();
		}

		Set<IVariant> set = scopedVariants.get(s);
		if (set == null) {
			set = HashUtil.getHashSet();
		}
		set.add(scoped);
		scopedVariants.put(s, set);

	}

	/**
	 * Returns the empty scope object.
	 * 
	 * @return the empty scope object
	 */
	public IScope getEmptyScope() {
		return emptyScope;
	}

	/**
	 * Method checks if the given topic is used as theme.
	 * 
	 * @param theme the theme
	 * @return <code>true</code> if the topic is used as theme,
	 *         <code>false</code> otherwise.
	 */
	public boolean usedAsTheme(ITopic theme) {
		if (scopes == null) {
			return false;
		}
		for (Entry<IScope, Set<ITopic>> entry : this.scopes.entrySet()) {
			if (entry.getValue().contains(theme) && !getScoped(entry.getKey()).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removing the theme and all dependent scopes and scoped objects.
	 * 
	 * @param theme the theme
	 * @return the removed scoped objects
	 */
	public Set<IScopable> removeScopes(ITopic theme) {

		Set<IScopable> removed = HashUtil.getHashSet();
		if (scopes != null) {
			Map<IScope, Set<ITopic>> copy = HashUtil.getHashMap(this.scopes);
			for (Entry<IScope, Set<ITopic>> entry : copy.entrySet()) {
				if (entry.getValue().contains(theme)) {
					IScope scope = entry.getKey();
					/*
					 * remove scoped associations
					 */
					for (IAssociation a : getScopedAssociations(scope)) {
						associationScopes.remove(a);
						removed.add(a);
					}
					if (scopedAssociations != null) {
						scopedAssociations.remove(scope);
					}
					/*
					 * remove scoped occurrences
					 */
					for (IOccurrence o : getScopedOccurrences(scope)) {
						occurrenceScopes.remove(o);
						removed.add(o);
					}
					if (scopedOccurrences != null) {
						scopedOccurrences.remove(scope);
					}
					/*
					 * remove scoped names
					 */
					for (IName n : getScopedNames(scope)) {
						nameScopes.remove(n);
						removed.add(n);
					}
					if (scopedNames != null) {
						scopedNames.remove(scope);
					}
					/*
					 * remove scoped variants
					 */
					for (IVariant v : getScopedVariants(scope)) {
						variantScopes.remove(v);
						removed.add(v);
					}
					if (scopedVariants != null) {
						scopedVariants.remove(scope);
					}

					/*
					 * remove scope
					 */
					scopes.remove(scope);
				}
			}
		}
		return removed;
	}

	/**
	 * {@inheritDoc}
	 */
	public void replace(ITopic topic, ITopic replacement, IRevision revision) {
		if (scopes != null) {
			Set<IScope> scopes = HashUtil.getHashSet(this.scopes.keySet());
			for (IScope s : scopes) {
				if (s instanceof ScopeImpl && s.containsTheme(topic)) {
					Set<ITopic> themes = HashUtil.getHashSet();
					themes.addAll(s.getThemes());
					themes.remove(topic);
					themes.add(replacement);
					IScope newScope = getScope(themes);
					/*
					 * replace as association scope
					 */
					if (scopedAssociations != null && scopedAssociations.containsKey(s)) {
						Set<IAssociation> set = HashUtil.getHashSet(scopedAssociations.get(s));
						for (IAssociation a : set) {
							associationScopes.put(a, newScope);
						}
						if (scopedAssociations.containsKey(newScope)) {
							set.addAll(scopedAssociations.get(newScope));
						}
						scopedAssociations.put(newScope, set);
					}
					/*
					 * replace as name scope
					 */
					if (scopedNames != null && scopedNames.containsKey(s)) {
						Set<IName> set = HashUtil.getHashSet(scopedNames.get(s));
						for (IName n : set) {
							nameScopes.put(n, newScope);
						}
						if (scopedNames.containsKey(newScope)) {
							set.addAll(scopedNames.get(newScope));
						}
						scopedNames.put(newScope, set);
					}
					/*
					 * replace as occurrence scope
					 */
					if (scopedOccurrences != null && scopedOccurrences.containsKey(s)) {
						Set<IOccurrence> set = HashUtil.getHashSet(scopedOccurrences.get(s));
						for (IOccurrence o : set) {
							occurrenceScopes.put(o, newScope);
						}
						if (scopedOccurrences.containsKey(newScope)) {
							set.addAll(scopedOccurrences.get(newScope));
						}
						scopedOccurrences.put(newScope, set);
					}
					/*
					 * replace as variant scope
					 */
					if (scopedVariants != null && scopedVariants.containsKey(s)) {
						Set<IVariant> set = HashUtil.getHashSet(scopedVariants.get(s));
						for (IVariant v : set) {
							variantScopes.put(v, newScope);
						}
						if (scopedVariants.containsKey(newScope)) {
							set.addAll(scopedVariants.get(newScope));
						}
						scopedVariants.put(newScope, set);
					}
				}
			}
		}
	}

	/**
	 * Return all scopes containing the given theme
	 * 
	 * @param theme the theme
	 * @return a collection of all themes
	 */
	public Set<IScope> getScopes(ITopic theme) {
		Set<IScope> set = HashUtil.getHashSet();
		if (scopes != null) {
			for (IScope s : scopes.keySet()) {
				if (s instanceof ScopeImpl && s.containsTheme(theme)) {
					set.add(s);
				}
			}
		}
		return set;
	}

	/**
	 * Return all scopes used by name items.
	 * 
	 * @return all scopes
	 */
	public Set<IScope> getNameScopes() {
		if (scopedNames == null) {
			return HashUtil.getHashSet();
		}
		return HashUtil.getHashSet(scopedNames.keySet());
	}

	/**
	 * Return all scopes used by occurrence items.
	 * 
	 * @return all scopes
	 */
	public Set<IScope> getOccurrenceScopes() {
		if (scopedOccurrences == null) {
			return HashUtil.getHashSet();
		}
		return HashUtil.getHashSet(scopedOccurrences.keySet());
	}

	/**
	 * Return all scopes used by association items.
	 * 
	 * @return all scopes
	 */
	public Set<IScope> getAssociationScopes() {
		if (scopedAssociations == null) {
			return HashUtil.getHashSet();
		}
		return HashUtil.getHashSet(scopedAssociations.keySet());
	}

	/**
	 * Return all scopes used by variant items.
	 * 
	 * @return all scopes
	 */
	public Set<IScope> getVariantScopes() {
		if (scopedVariants == null) {
			return HashUtil.getHashSet();
		}
		Set<IScope> set = HashUtil.getHashSet();
		for (Entry<IVariant, IScope> s : variantScopes.entrySet()) {
			Set<ITopic> themes = HashUtil.getHashSet(s.getValue().getThemes());
			themes.addAll(s.getKey().getParent().getScopeObject().getThemes());
			set.add(getScope(themes));
		}
		return set;
	}
}
