package io.coriolis.api.core.modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.coriolis.api.core.Ship;
import io.coriolis.api.core.modules.exceptions.UnknownIdException;
import io.coriolis.api.core.modules.exceptions.UnknownModuleException;
import io.coriolis.api.core.modules.exceptions.UnknownShipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public enum Modules {
    INSTANCE;

    final static Logger logger = LoggerFactory.getLogger(Modules.class);

    private ImmutableMap<String, String> groupToName = ImmutableMap.<String, String>builder()
            // Standard
            .put("pp", "power plant")
            .put("t", "thrusters")
            .put("fd", "frame shift drive")
            .put("ls", "life support")
            .put("pd", "power distributor")
            .put("s", "sensors")
            .put("ft", "fuel tank")
            // internal
            .put("fs", "fuel scoop")
            .put("sc", "scanner")
            .put("am", "auto field-maintenance unit")
            .put("cr", "cargo rack")
            .put("fi", "frame shift drive interdictor")
            .put("hb", "hatch breaker limpet controller")
            .put("hr", "hull reinforcement package")
            .put("rf", "refinery")
            .put("scb", "shield cell bank")
            .put("sg", "shield generator")
            .put("psg", "prismatic shield generator")
            .put("dc", "docking computer")
            .put("fx", "fuel transfer limpet controller")
            .put("pc", "prospector limpet controller")
            .put("cc", "collector limpet controller")
            // hard points
            .put("bl", "beam laser")
            .put("ul", "burst laser")
            .put("c", "cannon")
            .put("cs", "cargo scanner")
            .put("cm", "countermeasure")
            .put("fc", "fragment cannon")
            .put("ws", "frame shift wake scanner")
            .put("kw", "kill warrant scanner")
            .put("nl", "mine launcher")
            .put("ml", "mining laser")
            .put("mr", "missile rack")
            .put("pa", "plasma accelerator")
            .put("mc", "multi-cannon")
            .put("pl", "pulse laser")
            .put("rg", "rail gun")
            .put("sb", "shield booster")
            .put("tp", "torpedo pylon")
            .build();

    private ImmutableList<String> standardIds;
    private ImmutableList<String> internalIds;
    private ImmutableList<String> hardpointIds;
    private ImmutableList<String> utilityIds;

    private ImmutableMap<String, Integer> standardIdToIndex;
    private ImmutableMap<String, Integer> internalIdToIndex;
    private ImmutableMap<String, Integer> hardpointIdToIndex;
    private ImmutableMap<String, Integer> utilityIdToIndex;

    private ImmutableMap<String, Integer> standardEddbIdToIndex;
    private ImmutableMap<String, Integer> internaldEddbIdToIndex;
    private ImmutableMap<String, Integer> hardpointdEddbIdToIndex;
    private ImmutableMap<String, Integer> utilitydEddbIdToIndex;

    private ImmutableMap<String, Integer> standardNameToIndex;
    private ImmutableMap<String, Integer> internalNameToIndex;
    private ImmutableMap<String, Integer> hardpointNameToIndex;
    private ImmutableMap<String, Integer> utilityNameToIndex;

    public void initialize() {
        buildStandard();
        buildInternal();
        buildHardpointandUtility();
    }

    public int getStandardCount() {
        return standardIds.size();
    }

    public int getInternalCount() {
        return internalIds.size();
    }

    public int getHardpointCount() {
        return hardpointIds.size();
    }

    public int getUtilityCount() {
        return utilityIds.size();
    }

    public int getStandardIndexBy(String id) throws UnknownIdException {
        if(!standardIdToIndex.containsKey(id)) {
            throw new UnknownIdException(id);
        }
        return standardIdToIndex.get(id);
    }

    public int getStandardIndexBy(String name, String clazz, String rating, String ship) throws UnknownModuleException, UnknownShipException {
        String longName;

        if(Strings.isNullOrEmpty(ship)) {
            longName = name.toLowerCase() + clazz + rating;
        } else {
            longName = Ship.fromString(ship) + name.toLowerCase() + clazz + rating;
        }

        if(!standardNameToIndex.containsKey(longName)) {
            throw new UnknownModuleException(name, clazz, rating, null, null, ship);
        }

        // Normalize ship name
        return standardNameToIndex.get(longName);
    }

    public int getInternalIndexBy(String id) throws UnknownIdException {
        if(!internalIdToIndex.containsKey(id)) {
            throw new UnknownIdException(id);
        }
        return internalIdToIndex.get(id);
    }

    public int getInternalIndexBy(String name, String clazz, String rating) throws UnknownModuleException {
        String longName = name.toLowerCase() + clazz + rating;
        if(!internalNameToIndex.containsKey(longName)) {
            throw new UnknownModuleException(name, clazz, rating, null, null, null);
        }
        return internalNameToIndex.get(longName);
    }

    public int getHardpointIndexBy(String id) throws UnknownIdException {
        if(!hardpointIdToIndex.containsKey(id)) {
            throw new UnknownIdException(id);
        }
        return hardpointIdToIndex.get(id);
    }

    public int getHardpointIndexBy(String name, String clazz, String rating, String mount, String guidance) throws UnknownModuleException {
        String longName = name.toLowerCase() + clazz + rating + mount + guidance;
        if(!hardpointNameToIndex.containsKey(longName)) {
            throw new UnknownModuleException(name, clazz, rating, mount, guidance, null);
        }
        return hardpointNameToIndex.get(longName);
    }

    public int getUtilityIndexBy(String id) throws UnknownIdException {
        if(!utilityIdToIndex.containsKey(id)) {
            throw new UnknownIdException(id);
        }
        return utilityIdToIndex.get(id);
    }

    public int getUtilityIndexBy(String name, String clazz, String rating) throws UnknownModuleException {
        String longName = name.toLowerCase() + clazz + rating;
        if(!utilityNameToIndex.containsKey(longName)) {
            throw new UnknownModuleException(name, clazz, rating, null, null, null);
        }
        return utilityNameToIndex.get(longName);
    }

    public int getStandardIndexByEddbID(String eddbId) {
        if(!standardEddbIdToIndex.containsKey(eddbId)) {
            return -1;
        }
        return standardEddbIdToIndex.get(eddbId);
    }

    public int getInternalIndexByEddbID(String eddbId) {
        if(!internaldEddbIdToIndex.containsKey(eddbId)) {
            return -1;
        }
        return internaldEddbIdToIndex.get(eddbId);
    }

    public int getHardpointIndexByEddbID(String eddbId) {
        if(!hardpointdEddbIdToIndex.containsKey(eddbId)) {
            return -1;
        }
        return hardpointdEddbIdToIndex.get(eddbId);
    }

    public int getUtilityIndexByEddbID(String eddbId) {
        if(!utilitydEddbIdToIndex.containsKey(eddbId)) {
            return -1;
        }
        return utilitydEddbIdToIndex.get(eddbId);
    }

    public ModuleSet createStandardSet(){
        return new ModuleSet(getStandardCount());
    }

    public ModuleSet createInternalSet(){
        return new ModuleSet(getInternalCount());
    }

    public ModuleSet createHardpointSet(){
        return new ModuleSet(getHardpointCount());
    }

    public ModuleSet createUtilitySet(){
        return new ModuleSet(getUtilityCount());
    }

    public ModuleSet standardFromIdList(List<String> idList) throws UnknownIdException {
        return moduleSetFromIdList(getStandardCount(), idList, standardIdToIndex);
    }

    public ModuleSet internalFromIdList(List<String> idList) throws UnknownIdException {
        return moduleSetFromIdList(getInternalCount(), idList, internalIdToIndex);
    }

    public ModuleSet hardpointFromIdList(List<String> idList) throws UnknownIdException {
        return moduleSetFromIdList(getHardpointCount(), idList, hardpointIdToIndex);
    }

    public ModuleSet utilityFromIdList(List<String> idList) throws UnknownIdException {
        return moduleSetFromIdList(getUtilityCount(), idList, utilityIdToIndex);
    }

    public List<String> toStandardList(ModuleSet set) {
        return moduleSetToIdList(set, standardIds);
    }

    public List<String> toInternalList(ModuleSet set) {
        return moduleSetToIdList(set, internalIds);
    }

    public List<String> toHardPointList(ModuleSet set) {
        return moduleSetToIdList(set, hardpointIds);
    }

    public List<String> toUtilityList(ModuleSet set) {
        return moduleSetToIdList(set, utilityIds);
    }

    private List<String> moduleSetToIdList(ModuleSet ms, ImmutableList<String> ids) {
        List<String> idList = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            if (ms.has(i)) {
                idList.add(ids.get(i));
            }
        }
        return idList;
    }

    private ModuleSet moduleSetFromIdList (int setSize, List<String> idList, Map<String, Integer> idMap) throws UnknownIdException {
        if(idList == null || idList.size() == 0) {
            return null;
        }

        ModuleSet ms = new ModuleSet(setSize);

        for (String id : idList) {
            if (idMap.containsKey(id)) {
                ms.add(idMap.get(id));
            } else {
                throw  new UnknownIdException(id);
            }
        }
        return ms;
    }

    private void buildStandard() {
        String[] files = new String[]{"frame_shift_drive","fuel_tank","life_support","power_distributor","power_plant","sensors","thrusters"};
        ImmutableList.Builder<String> standardIdsBuilder = ImmutableList.builder();
        ImmutableMap.Builder<String, Integer> standardIdToIndexBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<String, Integer> standardEddbIdToIndexBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<String, Integer> standardNameToIndexBuilder = ImmutableMap.builder();
        ObjectMapper mapper = new ObjectMapper();
        int index = 0;
        String currentFile = null;


        try {
            for (String file : files) {
                currentFile = file;
                JsonNode root = mapper.readTree(getClass().getResourceAsStream("/data/components/standard/" + file + ".json"));
                for(Iterator<JsonNode> modules = root.elements(); modules.hasNext(); ) {
                    JsonNode module = modules.next();
                    String id = module.get("id").asText();
                    String eddbID = module.get("eddbID").asText();
                    String longName = (module.has("name") ? module.get("name").asText().toLowerCase() : groupToName.get(module.get("grp").asText()))
                            + module.get("class").asText()
                            + module.get("rating").asText();
                    standardIdsBuilder.add(id);
                    standardIdToIndexBuilder.put(id, index);
                    standardEddbIdToIndexBuilder.put(eddbID, index);
                    standardNameToIndexBuilder.put(longName, index);
                    index++;
                }
            }

            JsonNode root = mapper.readTree(getClass().getResourceAsStream("/data/components/bulkheads.json"));
            currentFile = "bulkheads.json";
            for(Iterator<String> ships = root.fieldNames(); ships.hasNext(); ) {
                String ship = ships.next();
                String canonicalShipName = Ship.fromString(ship).toString();
                for(Iterator<JsonNode> bulkheads = root.get(ship).elements(); bulkheads.hasNext(); ) {
                    JsonNode bulkhead = bulkheads.next();
                    String id = bulkhead.get("id").asText();
                    standardIdsBuilder.add(id);
                    standardIdToIndexBuilder.put(id, index);
                    standardNameToIndexBuilder.put(canonicalShipName + bulkhead.get("name").asText().toLowerCase() + bulkhead.get("class").asText() + bulkhead.get("rating").asText(), index);
                    index++;
                }
            }

        } catch (NullPointerException e) {
            logger.error("Unable to read Standard file: " + currentFile + " " + e.getMessage() );
        } catch (IOException e) {
            logger.error("Unable to read Standard file: " + currentFile + " " + e.getMessage() );
        } catch (UnknownShipException e) {
            logger.error("Unknown ship from bulkheads: " + e.getMessage() );
        }

        standardIds = standardIdsBuilder.build();
        standardIdToIndex = standardIdToIndexBuilder.build();
        standardEddbIdToIndex = standardEddbIdToIndexBuilder.build();
        standardNameToIndex = standardNameToIndexBuilder.build();
    }

    private void buildInternal() {
        String[] files = new String[]{
                "auto_field_maintenance_unit", "cargo_rack", "collector_limpet_controllers", "docking_computer", "frame_shift_drive_interdictor",
                "fuel_scoop", "fuel_transfer_limpet_controllers", "hatch_breaker_limpet_controller", "hull_reinforcement_package",
                "pristmatic_shield_generator", "prospector_limpet_controllers", "refinery", "scanner",
                "shield_cell_bank", "shield_generator"
        };
        ImmutableList.Builder<String> internalIdsBuilder = ImmutableList.builder();
        ImmutableBiMap.Builder<String, Integer> internalIdToIndexBuilder = ImmutableBiMap.builder();
        ImmutableBiMap.Builder<String, Integer> internalEddbIdToIndexBuilder = ImmutableBiMap.builder();
        ImmutableBiMap.Builder<String, Integer> internalNameToIndexBuilder = ImmutableBiMap.builder();
        ObjectMapper mapper = new ObjectMapper();
        int index = 0;

        try {
            for (String file : files) {
                JsonNode root = mapper.readTree(getClass().getResourceAsStream("/data/components/internal/" + file + ".json"));
                String grp = root.fieldNames().next();
                String groupName = groupToName.get(grp);
                root = root.get(grp);
                for(Iterator<JsonNode> modules = root.elements(); modules.hasNext(); ) {
                    JsonNode module = modules.next();
                    String id = module.get("id").asText();
                    String eddbID = module.get("eddbID").asText();
                    String longName = (module.has("name") ? module.get("name").asText().toLowerCase() : groupName)
                            + module.get("class").asText()
                            + module.get("rating").asText();
                    internalIdsBuilder.add(id);
                    internalIdToIndexBuilder.put(id, index);
                    internalEddbIdToIndexBuilder.put(eddbID, index);
                    internalNameToIndexBuilder.put(longName, index);
                    index++;
                }
            }
        } catch (IOException e) {
            logger.error("Unable to read Internal file: " + e.getMessage() );
        }

        internalIds = internalIdsBuilder.build();
        internalIdToIndex = internalIdToIndexBuilder.build();
        internaldEddbIdToIndex = internalEddbIdToIndexBuilder.build();
        internalNameToIndex = internalNameToIndexBuilder.build();
    }

    private void buildHardpointandUtility() {
        String[] files = new String[]{
                "beam_laser", "burst_laser", "cannon", "cargo_scanner", "countermeasures", "fragment_cannon", "frame_shift_wake_scanner",
                "kill_warrant_scanner", "mine_launcher", "mining_laser", "missile_rack", "multi_cannon", "plasma_accelerator", "pulse_laser",
                "rail_gun", "shield_booster", "torpedo_pylon"
        };
        ImmutableList.Builder<String> hardPointIdsBuilder = ImmutableList.builder();
        ImmutableBiMap.Builder<String, Integer> hardpointIdToIndexBuilder = ImmutableBiMap.builder();
        ImmutableBiMap.Builder<String, Integer> hardpointEddbIdToIndexBuilder = ImmutableBiMap.builder();
        ImmutableBiMap.Builder<String, Integer> hardpointNameToIndexBuilder = ImmutableBiMap.builder();
        ImmutableList.Builder<String> utilityIdsBuilder = ImmutableList.builder();
        ImmutableBiMap.Builder<String, Integer> utilityIdToIndexBuilder = ImmutableBiMap.builder();
        ImmutableBiMap.Builder<String, Integer> utilityEddbIdToIndexBuilder = ImmutableBiMap.builder();
        ImmutableBiMap.Builder<String, Integer> utilityNameToIndexBuilder = ImmutableBiMap.builder();
        ObjectMapper mapper = new ObjectMapper();
        int hpIndex = 0;
        int uIndex = 0;

        try {
            for (String file : files) {
                JsonNode root = mapper.readTree(getClass().getResourceAsStream("/data/components/hardpoints/" + file + ".json"));
                String grp = root.fieldNames().next();
                String groupName = groupToName.get(grp);
                root = root.get(grp);
                for(Iterator<JsonNode> modules = root.elements(); modules.hasNext(); ) {
                    JsonNode module = modules.next();
                    String id = module.get("id").asText();
                    String eddbID = module.get("eddbID").asText();
                    String longName = (module.has("name") ? module.get("name").asText().toLowerCase() : groupName)
                            + module.get("class").asText()
                            + module.get("rating").asText()
                            + (module.has("mode") ?  module.get("mode").asText() : "")
                            + (module.has("missile") ?  module.get("missile").asText() : "");

                    if (module.get("class").asInt() > 0) {
                        hardPointIdsBuilder.add(id);
                        hardpointIdToIndexBuilder.put(id, hpIndex);
                        hardpointNameToIndexBuilder.put(longName, hpIndex);
                        hardpointEddbIdToIndexBuilder.put(eddbID, hpIndex);
                        hpIndex++;
                    } else {
                        utilityIdsBuilder.add(id);
                        utilityIdToIndexBuilder.put(id, uIndex);
                        utilityEddbIdToIndexBuilder.put(eddbID, uIndex);
                        utilityNameToIndexBuilder.put(longName, uIndex);
                        uIndex++;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Unable to read Hardpoints file: " + e.getMessage() );
        }

        hardpointIds = hardPointIdsBuilder.build();
        hardpointIdToIndex = hardpointIdToIndexBuilder.build();
        hardpointdEddbIdToIndex = hardpointEddbIdToIndexBuilder.build();
        hardpointNameToIndex = hardpointNameToIndexBuilder.build();
        utilityIds = utilityIdsBuilder.build();
        utilityIdToIndex = utilityIdToIndexBuilder.build();
        utilitydEddbIdToIndex = utilityEddbIdToIndexBuilder.build();
        utilityNameToIndex = utilityNameToIndexBuilder.build();
    }

}
