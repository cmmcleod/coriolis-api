package io.coriolis.api.core;

import io.coriolis.api.core.modules.exceptions.UnknownShipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Created by cmmcleod on 10/13/15.
 */
public enum Ship {
    ADDER("Adder"),
    ANACONDA("Anaconda"),
    ASP_EXPLORER("Asp Explorer", "asp"),
    COBRA_MK_III("Cobra Mk III", "cobra_mk_iii"),
    DIAMONDBACK_EXPLORER("Diamondback Explorer", "diamondback_explorer"),
    DIAMONDBACK_SCOUT("Diamondback Scout", "diamondback", "diamondback_scout"),
    EAGLE("Eagle", "eagle mk ii"),
    FEDERAL_ASSAULT_SHIP("Federal Assault Ship", "federal_assault_ship"),
    FEDERAL_DROPSHIP("Federal Dropship", "federal_dropship"),
    FEDERAL_GUNSHIP("Federal Gunship", "federal_gunship"),
    FER_DE_LANCE("Fer-de-Lance", "fer_de_lance"),
    HAULER("Hauler"),
    IMPERIAL_CLIPPER("Imperial Clipper", "imperial_clipper"),
    IMPERIAL_COURIER("Imperial Courier", "imperial_courier"),
    IMPERIAL_EAGLE("Imperial Eagle", "imperial_eagle"),
    ORCA("Orca"),
    PYTHON("Python"),
    SIDEWINDER("Sidewinder", "sidewinder mk i"),
    TYPE_6_TRANSPORTER("Type-6 Transporter", "type_6_transporter"),
    TYPE_7_TRANSPORTER("Type-7 Transporter", "type_7_transporter", "type_7_transport"),
    TYPE_9_HEAVY("Type-9 Heavy", "type_9_heavy"),
    VIPER("Viper", "viper mk iii"),
    VULTURE("Vulture");

    final static Logger logger = LoggerFactory.getLogger(Ship.class);

    private int bit;
    private String name;
    private List<String> aliases;

    private Ship(String name){
        this.name = name;
        aliases = null;
    }

    private Ship(String name, String... aliases){
        this.name = name;
        this.aliases = Arrays.asList(aliases);
    }

    @Override
    public String toString() {
        return name;
    }

    public static Ship fromString(String name) throws UnknownShipException {
        name = name.trim().replace(".", "");
        for (Ship s : Ship.values()) {
            if (name.equalsIgnoreCase(s.toString()) || (s.aliases != null && s.aliases.contains(name.toLowerCase()))) {
                return s;
            }
        }
        throw new UnknownShipException(name);
    }

    public static EnumSet<Ship> from(Collection<String> shipSet) throws UnknownShipException {
        if (shipSet == null) {
            return null;
        }

        EnumSet<Ship> ships = EnumSet.noneOf(Ship.class);

        for(String s : shipSet) {
            Ship ship = fromString(s);
            if (ship != null) {
                ships.add(ship);
            }
        }

        return ships;
    }
}
