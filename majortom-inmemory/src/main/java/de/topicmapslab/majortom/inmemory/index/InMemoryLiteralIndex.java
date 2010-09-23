package de.topicmapslab.majortom.inmemory.index;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import org.tmapi.core.Locator;
import org.tmapi.core.Name;
import org.tmapi.core.Occurrence;
import org.tmapi.core.TMAPIRuntimeException;
import org.tmapi.core.Variant;

import de.topicmapslab.geotype.wgs84.Wgs84Coordinate;
import de.topicmapslab.majortom.index.nonpaged.CachedLiteralIndexImpl;
import de.topicmapslab.majortom.inmemory.store.InMemoryTopicMapStore;
import de.topicmapslab.majortom.model.core.ICharacteristics;
import de.topicmapslab.majortom.model.core.IDatatypeAware;
import de.topicmapslab.majortom.model.core.ILocator;
import de.topicmapslab.majortom.model.core.IName;
import de.topicmapslab.majortom.model.core.IOccurrence;
import de.topicmapslab.majortom.model.core.IVariant;
import de.topicmapslab.majortom.model.index.ILiteralIndex;
import de.topicmapslab.majortom.util.HashUtil;
import de.topicmapslab.majortom.util.LiteralUtils;
import de.topicmapslab.majortom.util.XmlSchemeDatatypes;

/**
 * Implementation of the {@link ILiteralIndex}
 * 
 * @author Sven Krosse
 * 
 */
public class InMemoryLiteralIndex extends
		CachedLiteralIndexImpl<InMemoryTopicMapStore> {

	/**
	 * constructor
	 * 
	 * @param store
	 *            the in-memory store
	 */
	public InMemoryLiteralIndex(InMemoryTopicMapStore store) {
		super(store);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetBooleans(boolean value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_BOOLEAN))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& datatypeAware.booleanValue().equals(value)) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetCharacteristics(String value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_STRING))) {
			if (datatypeAware instanceof IOccurrence
					&& datatypeAware.getValue().equals(value)) {
				set.add((IOccurrence) datatypeAware);
			}
		}
		for (IName n : getStore().getCharacteristicsStore().getNames()) {
			if (n.getValue().equalsIgnoreCase(value)) {
				set.add(n);
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetCharacteristics(Locator datatype) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (datatype == null) {
			throw new IllegalArgumentException("Datatype cannot be null.");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		set.addAll(getStore().getCharacteristicsStore().getCharacteristics());
		set.retainAll(getStore().getCharacteristicsStore().getDatatypeAwares(
				(ILocator) datatype));
		if (datatype.getReference().equalsIgnoreCase(
				XmlSchemeDatatypes.XSD_STRING)) {
			set.addAll(getStore().getCharacteristicsStore().getNames());
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetCharacteristics(String value,
			Locator datatype) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		if (datatype == null) {
			throw new IllegalArgumentException("Datatype cannot be null.");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						(ILocator) datatype)) {
			if (datatypeAware instanceof IOccurrence
					&& datatypeAware.getValue().equals(value)) {
				set.add((IOccurrence) datatypeAware);
			}
		}
		if (datatype.getReference().equalsIgnoreCase(
				XmlSchemeDatatypes.XSD_STRING)) {
			for (IName name : getStore().getCharacteristicsStore().getNames()) {
				if (name.getValue().equalsIgnoreCase(value)) {
					set.add(name);
				}
			}
		}

		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetCharacteristicsMatches(
			String regExp) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (regExp == null) {
			throw new IllegalArgumentException(
					"Regular expression cannot be null.");
		}
		return Collections
				.unmodifiableCollection(getCharacteristicsMatches(Pattern
						.compile(regExp)));
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetCharacteristicsMatches(
			String regExp, Locator datatype) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (regExp == null) {
			throw new IllegalArgumentException(
					"Regular expression cannot be null.");
		}
		if (datatype == null) {
			throw new IllegalArgumentException("Datatype cannot be null.");
		}
		return Collections.unmodifiableCollection(getCharacteristicsMatches(
				Pattern.compile(regExp), datatype));
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetCharacteristicsMatches(
			Pattern regExp) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (regExp == null) {
			throw new IllegalArgumentException(
					"Regular expression cannot be null.");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();

		for (IName n : getStore().getCharacteristicsStore().getNames()) {
			if (regExp.matcher(n.getValue()).matches()) {
				set.add(n);
			}
		}
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_STRING))) {
			if (datatypeAware instanceof IOccurrence
					&& regExp.matcher(datatypeAware.getValue()).matches()) {
				set.add((IOccurrence) datatypeAware);
			}
		}

		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetCharacteristicsMatches(
			Pattern regExp, Locator datatype) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (regExp == null) {
			throw new IllegalArgumentException(
					"Regular expression cannot be null.");
		}
		if (datatype == null) {
			throw new IllegalArgumentException("Datatype cannot be null.");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						(ILocator) datatype)) {
			if (datatypeAware instanceof IOccurrence
					&& regExp.matcher(datatypeAware.getValue()).matches()) {
				set.add((IOccurrence) datatypeAware);
			}
		}
		if (datatype.getReference().equalsIgnoreCase(
				XmlSchemeDatatypes.XSD_STRING)) {
			for (IName n : getStore().getCharacteristicsStore().getNames()) {
				if (regExp.matcher(n.getValue()).matches()) {
					set.add(n);
				}
			}
		}

		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetCoordinates(Wgs84Coordinate value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.WGS84_COORDINATE))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& datatypeAware.coordinateValue().equals(value)) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetCoordinates(Wgs84Coordinate value,
			double deviance) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.WGS84_COORDINATE))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& value.getDistance(datatypeAware.coordinateValue()) <= deviance) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetDateTime(Calendar value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_DATETIME))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& datatypeAware.dateTimeValue().equals(value)) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetDateTime(Calendar value,
			Calendar deviance) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		if (deviance == null) {
			throw new IllegalArgumentException("Deviance cannot be null.");
		}

		double deviance_ = ((double) (deviance.get(Calendar.SECOND) + (deviance
				.get(Calendar.MINUTE) + (deviance.get(Calendar.HOUR) + (deviance
				.get(Calendar.DAY_OF_MONTH) + (deviance.get(Calendar.MONTH) + deviance
				.get(Calendar.YEAR) * 12) * 30) * 24) * 60) * 60)) * 1000;

		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_DATETIME))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& LiteralUtils.inRange(datatypeAware.dateTimeValue(),
								value, deviance_)) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetDoubles(double value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_DOUBLE))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& datatypeAware.doubleValue().equals(value)) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetDoubles(double value,
			double deviance) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_DOUBLE))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& Math.abs(datatypeAware.doubleValue() - value) <= deviance) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetFloats(float value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_FLOAT))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& datatypeAware.floatValue() == value) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetFloats(float value, double deviance) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_FLOAT))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& Math.abs(datatypeAware.floatValue() - value) <= deviance) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetIntegers(int value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_INT))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& datatypeAware.intValue() == value) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetIntegers(int value, double deviance) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_INT))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& Math.abs(datatypeAware.intValue() - value) <= deviance) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetLongs(long value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_LONG))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& datatypeAware.longValue() == value) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetLongs(long value, double deviance) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_LONG))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& Math.abs(datatypeAware.longValue() - value) <= deviance) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<ICharacteristics> doGetUris(URI value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		Set<ICharacteristics> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_ANYURI))) {
			try {
				if (datatypeAware instanceof IOccurrence
						&& datatypeAware.uriValue().equals(value)) {
					set.add((IOccurrence) datatypeAware);
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Name> doGetNames(String value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		Set<Name> set = HashUtil.getHashSet();
		for (IName n : getStore().getCharacteristicsStore().getNames()) {
			if (n.getValue().equalsIgnoreCase(value)) {
				set.add(n);
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Occurrence> doGetOccurrences(String value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		Set<Occurrence> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_STRING))) {
			if (datatypeAware instanceof IOccurrence
					&& datatypeAware.getValue().equals(value)) {
				set.add((IOccurrence) datatypeAware);
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Occurrence> doGetOccurrences(Locator value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		Set<Occurrence> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_ANYURI))) {
			if (datatypeAware instanceof IOccurrence
					&& datatypeAware.locatorValue().equals(value)) {
				set.add((IOccurrence) datatypeAware);
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Occurrence> doGetOccurrences(String value,
			Locator datatype) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		if (datatype == null) {
			throw new IllegalArgumentException("Datatype cannot be null.");
		}
		Set<Occurrence> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						(ILocator) datatype)) {
			if (datatypeAware instanceof IOccurrence
					&& datatypeAware.getValue().equalsIgnoreCase(value)) {
				set.add((IOccurrence) datatypeAware);
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Variant> doGetVariants(String value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		Set<Variant> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_STRING))) {
			if (datatypeAware instanceof IVariant
					&& datatypeAware.getValue().equals(value)) {
				set.add((IVariant) datatypeAware);
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Variant> doGetVariants(Locator value) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		Set<Variant> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						getStore().getIdentityStore().createLocator(
								XmlSchemeDatatypes.XSD_ANYURI))) {
			if (datatypeAware instanceof IVariant
					&& datatypeAware.locatorValue().equals(value)) {
				set.add((IVariant) datatypeAware);
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Variant> doGetVariants(String value, Locator datatype) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		if (datatype == null) {
			throw new IllegalArgumentException("Datatype cannot be null.");
		}
		Set<Variant> set = HashUtil.getHashSet();
		for (IDatatypeAware datatypeAware : getStore()
				.getCharacteristicsStore().getDatatypeAwares(
						(ILocator) datatype)) {
			if (datatypeAware instanceof IVariant
					&& datatypeAware.getValue().equalsIgnoreCase(value)) {
				set.add((IVariant) datatypeAware);
			}
		}
		return Collections.unmodifiableCollection(set);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<IDatatypeAware> doGetDatatypeAwares(Locator dataType) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		if (dataType == null) {
			throw new IllegalArgumentException("Datatype cannot be null.");
		}
		if (!(dataType instanceof ILocator)) {
			throw new IllegalArgumentException(
					"Datatype has to be created by this topic map.");
		}
		return HashUtil.getHashSet(getStore().getCharacteristicsStore()
				.getDatatypeAwares((ILocator) dataType));
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Name> doGetNames() {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Collection<Name> col = HashUtil.getHashSet();
		col.addAll(getStore().getCharacteristicsStore().getNames());
		return col;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Occurrence> doGetOccurrences() {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Collection<Occurrence> col = HashUtil.getHashSet();
		col.addAll(getStore().getCharacteristicsStore().getOccurrences());
		return col;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Variant> doGetVariants() {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		Collection<Variant> col = HashUtil.getHashSet();
		col.addAll(getStore().getCharacteristicsStore().getVariants());
		return col;
	}

}
