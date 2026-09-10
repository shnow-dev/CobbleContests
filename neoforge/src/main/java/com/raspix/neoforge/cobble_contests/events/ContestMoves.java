package com.raspix.neoforge.cobble_contests.events;

import com.google.gson.*;
import com.raspix.common.cobble_contests.CobbleContests;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ContestMoves {

    public static ContestMoves instance;

    public Map<String, MoveData> contestMoves;


    public ContestMoves() {
        loadFromJson();
    }

    public void loadFromJson() {
        new GsonBuilder();
        Gson gson = new GsonBuilder().create();
        //MoveData
        String jsonString = "";


        //this.config = CobblemonConfig.GSON.fromJson(fileReader, CobblemonConfig::class.java)


        //ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        //try{
            /**String CONFIG_PATH = "config/$MODID/main.json";
            ResourceLocation jsonLocation = new ResourceLocation(CobbleContestsForge.MOD_ID, "contest_moves.json");
            String str = "main/resources/assets/contest_moves.json";//jsonLocation.getPath();
            System.out.println(str);
            File configFile = new File(jsonLocation.getPath());
            System.out.println(configFile.getAbsolutePath());
            FileReader fileReader = new FileReader(configFile);*/

            //Optional<Resource> resource = resourceManager.getResource(jsonLocation);
            //InputStream inputStream = (InputStream) resource.stream();
            // Read the input stream into a string
            //jsonString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            //fileReader.close();
        //} catch (IOException e) {
            //e.printStackTrace();
        //}
        /**try {
            contestMoves = new HashMap<>();
            ResourceLocation jsonLocation = new ResourceLocation(CobbleContestsForge.MOD_ID, "contest_moves.json");
            InputStream inputStream = Minecraft.getInstance().getResourceManager().getResource(jsonLocation).get().open();
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(new InputStreamReader(inputStream)).getAsJsonObject();
            JsonArray movesArray = jsonObject.getAsJsonArray("moves");
            for (int i = 0; i < movesArray.size(); i++) {
                JsonObject moveObject = movesArray.get(i).getAsJsonObject();
                String moveName = moveObject.get("name").getAsString();
                String contestStat = moveObject.get("contestStat").getAsString();
                int appeal = moveObject.get("appeal").getAsInt();
                //System.out.println("Move:" + moveName);
                contestMoves.put(moveName, new MoveData(contestStat, appeal));
            }
        }catch (IOException e){
            System.out.println("failed");
            e.printStackTrace();
        }*/
    }

    public static class MoveData{
        String name;
        String type;
        int appeal;

        public MoveData(String name, String type, int appeal){
            this.name = name;
            this.type = type;
            this.appeal = appeal;
        }

        public String getType(){
            return type;
        }

        public int getAppeal(){
            return appeal;
        }

        public String getName(){
            return name;
        }

    }

}
