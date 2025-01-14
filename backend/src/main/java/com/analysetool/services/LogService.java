package com.analysetool.services;

import com.analysetool.Application;
import com.analysetool.modells.*;
import com.analysetool.repositories.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogService {

    private PostRepository postRepository;
    private PostStatsRepository statsRepo;
    private TagStatRepository tagStatRepo;
    private WpTermRelationshipsRepository termRelRepo;
    private WPTermRepository termRepo;
    private WpTermTaxonomyRepository termTaxRepo;
    private WPUserRepository wpUserRepo;
    private UserStatsRepository userStatsRepo;

    private CommentsRepository commentRepo;
    private SysVarRepository sysVarRepo;
    private BufferedReader br;
    private String path = "";
    //^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}) regex für ip matching
    private String BlogSSPattern = ".*GET /blog/(\\S+).*s="; //search +1, view +1,(bei match) vor blog view pattern
    private String ArtikelSSPattern = ".*GET /artikel/(\\S+).*s=";//search +1, view +1,(bei match) vor artikel view pattern
    //private String BlogViewPattern = "^.*GET \/blog\/.* HTTP/1\\.1\" 200 .*$\n";//Blog view +1 bei match
    private String BlogViewPattern = ".*GET /blog/(\\S+)";
    private String RedirectPattern = "/.*GET .*goto=.*\"(https?:/.*/(artikel|blog|pressemitteilung)/(\\S*)/)";
    private String RedirectUserPattern ="/.*GET .*goto=.*\"(https?:/.*/(user)/(\\S*)/)";
    private String UserViewPattern=".*GET /user/(\\S+)/";

    //Blog view +1 bei match
    private String ArtikelViewPattern = ".*GET /artikel/(\\S+)";//Artikel view +1 bei match
    private String PresseViewPatter = ".*GET /pressemitteilung/(\\S+)/";
    private String PresseSSViewPatter = ".*GET /pressemitteilung/(\\S+)/*s=";
    Pattern pattern1_1 = Pattern.compile(ArtikelViewPattern);
    Pattern pattern1_2 = Pattern.compile(ArtikelSSPattern);
    Pattern pattern2_1 = Pattern.compile(BlogViewPattern);
    Pattern pattern2_2 = Pattern.compile(BlogSSPattern);
    Pattern pattern3=Pattern.compile(RedirectPattern);
    Pattern pattern4=Pattern.compile(UserViewPattern);
    Pattern pattern5_1 = Pattern.compile(PresseViewPatter);
    Pattern pattern5_2= Pattern.compile(PresseSSViewPatter);
    Pattern pattern4_2=Pattern.compile(RedirectUserPattern);
    private String lastLine = "";
    private int lineCounter = 0;
    private int lastLineCounter = 0;
    private boolean liveScanning ;
    //private String Pfad=Application.class.getClassLoader().getResource("access.log").getPath();
    private String Pfad = Paths.get(Application.class.getClassLoader().getResource("access.log").toURI()).toString();

    private Calendar kalender = Calendar.getInstance();
    private int aktuellesJahr = kalender.get(Calendar.YEAR);

    @Autowired
    public LogService(PostRepository postRepository, PostStatsRepository PostStatsRepository, TagStatRepository tagStatRepo, WpTermRelationshipsRepository termRelRepo, WPTermRepository termRepo, WpTermTaxonomyRepository termTaxRepo, WPUserRepository wpUserRepo, UserStatsRepository userStatsRepo, CommentsRepository commentRepo, SysVarRepository sysVarRepo) throws URISyntaxException {
        this.postRepository = postRepository;
        this.statsRepo = PostStatsRepository;
        this.tagStatRepo=tagStatRepo;
        this.termRelRepo=termRelRepo;
        this.termRepo=termRepo;
        this.termTaxRepo=termTaxRepo;
        this.wpUserRepo=wpUserRepo;
        this.userStatsRepo=userStatsRepo;
        this.commentRepo=commentRepo;
        this.sysVarRepo=sysVarRepo;
    }
    public LocalDateTime getCreationDateOfAccessLog(String filePath) {
        try {
            Path file = Paths.get(filePath);
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            System.out.println(LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault()));
            return LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
        } catch (Exception e) {
            // Fehlerbehandlung, falls das Erstelldatum nicht geladen werden kann
            e.printStackTrace();
            return null;
        }
    }

    @PostConstruct
    public void init() {
        SysVar SystemVariabeln = new SysVar();
        if(sysVarRepo.findAll().isEmpty()){


            SystemVariabeln.setDate(LocalDateTime.now());
            SystemVariabeln.setDayInYear(LocalDateTime.now().getDayOfYear());
            SystemVariabeln.setDayInWeek(LocalDateTime.now().getDayOfWeek().getValue());
            SystemVariabeln.setDayInMonth(LocalDateTime.now().getDayOfMonth());
            SystemVariabeln.setLastLine("");
            SystemVariabeln.setLastLineCount(0);
            SystemVariabeln.setLogDate(getCreationDateOfAccessLog(Pfad));
            liveScanning = false ;

        }else {SystemVariabeln = sysVarRepo.findAll().get(sysVarRepo.findAll().size()-1);

            //if(SystemVariabeln.getDate().getDayOfYear()!=(LocalDateTime.now().getDayOfYear())){
                liveScanning= (getCreationDateOfAccessLog(Pfad).withSecond(0).withNano(0).equals(SystemVariabeln.getLogDate().withSecond(0).withNano(0)));
                System.out.println(getCreationDateOfAccessLog(Pfad).withSecond(0).withNano(0)+ "  "+SystemVariabeln.getLogDate().withSecond(0).withNano(0));

                SystemVariabeln.setDate(LocalDateTime.now());
                SystemVariabeln.setDayInYear(LocalDateTime.now().getDayOfYear());
                SystemVariabeln.setDayInWeek(LocalDateTime.now().getDayOfWeek().getValue());
                SystemVariabeln.setDayInMonth(LocalDateTime.now().getDayOfMonth());
                SystemVariabeln.setLastLine("");
                if(!liveScanning){SystemVariabeln.setLastLineCount(0);}
                SystemVariabeln.setLogDate(getCreationDateOfAccessLog(Pfad));
           // }

        }


        run(liveScanning,Pfad, SystemVariabeln);

    }
    @Scheduled(cron = "0 0 * * * *") //einmal die Stunde
    //@Scheduled(cron = "0 */2 * * * *") //alle 2min
    public void runScheduled() {
        SysVar SystemVariabeln = new SysVar();
        if(sysVarRepo.findAll().isEmpty()){


            SystemVariabeln.setDate(LocalDateTime.now());
            SystemVariabeln.setDayInYear(LocalDateTime.now().getDayOfYear());
            SystemVariabeln.setDayInWeek(LocalDateTime.now().getDayOfWeek().getValue());
            SystemVariabeln.setDayInMonth(LocalDateTime.now().getDayOfMonth());
            SystemVariabeln.setLastLine("");
            SystemVariabeln.setLastLineCount(0);
            SystemVariabeln.setLogDate(getCreationDateOfAccessLog(Pfad));
            liveScanning = false;

        }else {SystemVariabeln = sysVarRepo.findAll().get(sysVarRepo.findAll().size()-1);

           // if(SystemVariabeln.getDate().getDayOfYear()!=(LocalDateTime.now().getDayOfYear())){
            liveScanning= (getCreationDateOfAccessLog(Pfad).withSecond(0).withNano(0).equals(SystemVariabeln.getLogDate().withSecond(0).withNano(0)));
                SystemVariabeln.setDate(LocalDateTime.now());
                SystemVariabeln.setDayInYear(LocalDateTime.now().getDayOfYear());
                SystemVariabeln.setDayInWeek(LocalDateTime.now().getDayOfWeek().getValue());
                SystemVariabeln.setDayInMonth(LocalDateTime.now().getDayOfMonth());
                SystemVariabeln.setLastLine("");
                if(!liveScanning){SystemVariabeln.setLastLineCount(0);}
                SystemVariabeln.setLogDate(getCreationDateOfAccessLog(Pfad));
          //  }

        }


        run(liveScanning,Pfad, SystemVariabeln);
    }
    public void run(boolean liveScanning, String path,SysVar SystemVariabeln)  {
        this.liveScanning = liveScanning;
        this.path = path;
        lastLineCounter=SystemVariabeln.getLastLineCount();
        lastLine = SystemVariabeln.getLastLine();
        lineCounter = 0 ;
        try  {
            br = new BufferedReader(new FileReader(path));
            findAMatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
        SystemVariabeln.setLastLineCount(lastLineCounter);
        SystemVariabeln.setLastLine(lastLine);
        sysVarRepo.save(SystemVariabeln);
    }

    public void findAMatch() throws IOException {
        String line;

        boolean foundPattern = false;

        while ((line = br.readLine()) != null ) {
            if(lineCounter!=lastLineCounter){
                System.out.println("Counting up");
            while(lineCounter!=lastLineCounter && liveScanning){
                br.readLine();
                lineCounter++;

            }
            System.out.println("reached final position");
            }

           // if (foundPattern) {
                Matcher matcher1_1 = pattern1_1.matcher(line);

                if (matcher1_1.find()) {
                    Matcher matcher1_2 = pattern1_2.matcher(line);

                    foundPattern = true;
                    if (matcher1_2.find()) {
                        // Do something with the matched 1.2 patterns
                        //System.out.println(line+"SEARCH FOUND");
                        processLine(line,2,matcher1_2);
                        foundPattern = true;
                    } else {//1.1 matched
                        //System.out.println(line+"NO SEARCH");
                        processLine(line,1,matcher1_1);
                        foundPattern = true;
                    }
                }
           // }
            else {
                    Matcher matcher2_1 = pattern2_1.matcher(line);

                    if (matcher2_1.find()) {
                        Matcher matcher2_2 = pattern2_2.matcher(line);

                        if (matcher2_2.find()) {
                            // Do something with the matched 2.2 patterns
                            processLine(line, 4, matcher2_2);
                            foundPattern = false;
                           // System.out.println(line+" SEARCH FOUND");
                        } else {
                            //2.1 match
                            processLine(line, 3, matcher2_1);
                            foundPattern = false;
                           // System.out.println(line+" NO SEARCH");
                        }
                    }else {
                        Matcher matcher5_1 = pattern5_1.matcher(line);

                        if (matcher5_1.find()) {
                            Matcher matcher5_2 = pattern5_2.matcher(line);

                            if (matcher5_2.find()) {
                                // Do something with the matched 2.2 patterns
                                processLine(line, 8, matcher5_2);
                                foundPattern = false;
                                // System.out.println(line+" SEARCH FOUND");
                            } else {
                                //2.1 match
                                processLine(line, 7, matcher5_1);
                                foundPattern = false;
                                // System.out.println(line+" NO SEARCH");
                            }
                        }
                    }
                }

            Matcher matcher3=pattern3.matcher(line);
            if(matcher3.find()){
                processLine(line,5,matcher3);
                }
            Matcher matcher4=pattern4.matcher(line);
            if(matcher4.find()){
                processLine(line,6,matcher4);
            }
            Matcher matcher4_2=pattern4_2.matcher(line);
            if(matcher4_2.find()){
                processLine(line,9,matcher4);
            }



            lineCounter++;
            lastLineCounter++;
            //System.out.println(lineCounter+" "+lastLine);
            //br.readLine();

        }
        //updateuseraktivität
        System.out.println("UPDATING USER ACTIVITY");
        updateUserActivity((long)3);
        updateUserStatsForAllUsers();
        lineCounter = 0 ;
        System.out.println("END OF LOG");
    }


    public void processLine(String line,int patternNumber,Matcher matcher){
        lastLine=line;
        if (patternNumber==1){

            System.out.println(postRepository.getIdByName(matcher.group(1).substring(0,matcher.group(1).length()-1))+matcher.group(1).substring(0,matcher.group(1).length()-1)+" PROCESSING 1.1");
            UpdatePerformanceAndViews(matcher);
        }
        if (patternNumber==2){

            System.out.println(postRepository.getIdByName(matcher.group(1).substring(0,matcher.group(1).length()-1))+matcher.group(1).substring(0,matcher.group(1).length()-1)+" PROCESSING 1.2");
            updatePerformanceViewsSearchSuccess(matcher);
        }
        if (patternNumber==3){

            System.out.println(postRepository.getIdByName(matcher.group(1).substring(0,matcher.group(1).length()-1))+matcher.group(1).substring(0,matcher.group(1).length()-1)+" PROCESSING 2.1");
            UpdatePerformanceAndViews(matcher);
        }
        if (patternNumber==4){

            System.out.println(postRepository.getIdByName(matcher.group(1).substring(0,matcher.group(1).length()-1))+matcher.group(1).substring(0,matcher.group(1).length()-1)+" PROCESSING 2.2");
            updatePerformanceViewsSearchSuccess(matcher);
        }

        if(patternNumber==5){
            System.out.println(matcher.group(3)+" PROCESSING 3");
            //gibts das PostStats objekt? -nein = neues -ja = updaten
            long id =postRepository.getIdByName(matcher.group(3));
            if (statsRepo.existsByArtIdAndYear(id,aktuellesJahr)){
                PostStats stats=statsRepo.findByArtIdAndAndYear(id,aktuellesJahr);
                long views = stats.getClicks();
                long refferings =stats.getRefferings();
                refferings++;
                float article_reffering_rate= ((float)refferings/views);
                System.out.println("RefRate :"+article_reffering_rate);
                stats.setClicks(views);
                stats.setReferrings(refferings);
                statsRepo.save(stats);
                //statsRepo.updateRefferingsAndRateByArtId(article_reffering_rate,refferings,id);

            }else{  statsRepo.save(new PostStats(id,(float) 0,(float) 0,0,0,1,(float) 0));
            }

        }

        if(patternNumber==6){
            System.out.println(matcher.group(1).replace("+","-")+" PROCESSING 4");
            if(wpUserRepo.findByNicename(matcher.group(1).replace("+","-")).isPresent()){
                updateUserStats(wpUserRepo.findByNicename(matcher.group(1).replace("+","-")).get());
            };
        }
        if (patternNumber==7){

            System.out.println(postRepository.getIdByName(matcher.group(1)+" "+matcher.group(1))+" PROCESSING 5.1");
            try{

                long id =postRepository.getIdByName(matcher.group(1));
                //hier nach TagSuchen WIP

                checkTheTag(id,false);
                if (statsRepo.existsByArtIdAndYear(id,aktuellesJahr)){
                    PostStats stats = statsRepo.findByArtIdAndAndYear(id,aktuellesJahr);
                    long views = stats.getClicks();
                    views ++;

                    LocalDateTime PostTimestamp = postRepository.getPostDateById(id);
                    LocalDateTime Now =  LocalDateTime.now();
                    Duration duration = Duration.between(PostTimestamp, Now);
                    long diffInDays = duration.toDays();
                    float Performance = views;
                    if (diffInDays>0&&views > 0){
                        Performance = (float)views/diffInDays;
                    }
                    stats.setClicks(views);
                    stats.setPerformance(Performance);

                    //statsRepo.updateClicksAndPerformanceByArtId(views,id,Performance);
                    //updateDailyClicks(id);
                    statsRepo.save(stats);
                    erhoeheWertFuerHeutigesDatum( id);
                }else{  statsRepo.save(new PostStats(id,(float) 0,(float) 0,1,0,0,(float) 0)); //updateDailyClicks(id);
                    erhoeheWertFuerHeutigesDatum( id);}
            }
            catch(Exception e){
                System.out.println("IGNORE "+matcher.group(1)+" BECAUSE: "+e.getMessage());
            }
        }
        if (patternNumber==8){

            System.out.println(postRepository.getIdByName(matcher.group(1))+matcher.group(1)+" PROCESSING 5.2");
            try{
                long id =postRepository.getIdByName(matcher.group(1));
                checkTheTag(id,true);
                if (statsRepo.existsByArtIdAndYear(id,aktuellesJahr)){
                    PostStats stats = statsRepo.findByArtIdAndAndYear(id,aktuellesJahr);
                    long views = stats.getClicks();
                    views ++;
                    long searchSuccess= stats.getSearchSuccess();
                    searchSuccess ++;
                    LocalDateTime PostTimestamp = postRepository.getPostDateById(id);
                    LocalDateTime Now =  LocalDateTime.now();
                    Duration duration = Duration.between(PostTimestamp, Now);
                    long diffInDays = duration.toDays();
                    float Performance = views;
                    if (diffInDays>0&&views > 0){
                        Performance = (float)views/diffInDays;
                    }
                    stats.setPerformance(Performance);
                    stats.setSearchSucces(searchSuccess);
                    stats.setClicks(views);
                    stats.setSearchSuccessRate((float)searchSuccess/views);
                    statsRepo.save(stats);
                   // statsRepo.updateClicksSearchSuccessRateAndPerformance(id,views,searchSuccess,Performance);
                   // updateDailyClicks(id);
                    erhoeheWertFuerHeutigesDatum( id);
                }else{  statsRepo.save(new PostStats(id,(float) 0,(float) 0,1,1,0,(float) 0)); //updateDailyClicks(id);
                    erhoeheWertFuerHeutigesDatum( id);}
            }
            catch(Exception e){
                System.out.println("IGNORE "+matcher.group(1)+" BECAUSE: "+e.getMessage());

            }}

        if(patternNumber==9){
            System.out.println(matcher.group(1).replace("+","-")+" PROCESSING 4_2");
            if(wpUserRepo.findByNicename(matcher.group(1).replace("+","-")).isPresent()){
                WPUser wpUser=wpUserRepo.findByNicename(matcher.group(1).replace("+","-")).get();
                if(userStatsRepo.existsByUserId(wpUser.getId())){
                    UserStats userStats = userStatsRepo.findByUserId(wpUser.getId());
                    long refferings = userStats.getRefferings();
                    long views = userStats.getProfileView();
                    refferings ++;
                    userStats.setRefferings(refferings);
                    if(views!=0){
                        userStats.setRefferingRate((float)refferings/views);
                    }
                    userStatsRepo.save(userStats);
                }else{
                    userStatsRepo.save(new UserStats(wpUser.getId(), (float) 0,(float) 0, 0,(float) 0,(float) 0,(float)0,(long)1));
                }

            };
        }


    }

    public void updatePerformanceViewsSearchSuccess(Matcher matcher) {
        try{
            long id =postRepository.getIdByName(matcher.group(1).substring(0,matcher.group(1).length()-1));
            checkTheTag(id,true);
            if (statsRepo.existsByArtIdAndYear(id,aktuellesJahr)){
                PostStats stats = statsRepo.findByArtIdAndAndYear(id,aktuellesJahr);
                long views = stats.getClicks();
                views ++;
                long searchSuccess= stats.getSearchSuccess();
            searchSuccess ++;
            LocalDateTime PostTimestamp = postRepository.getPostDateById(id);
            LocalDateTime Now =  LocalDateTime.now();
            Duration duration = Duration.between(PostTimestamp, Now);
            long diffInDays = duration.toDays();
            float Performance = views;
            if (diffInDays>0&&views > 0){
                Performance = (float)views/diffInDays;
            }
            statsRepo.updateClicksSearchSuccessRateAndPerformance(id,views,searchSuccess,Performance);
           // updateDailyClicks(id);
            erhoeheWertFuerHeutigesDatum( id);
        }else{  statsRepo.save(new PostStats(id,(float) 0,(float) 0,1,1,0,(float) 0));
            //updateDailyClicks(id);
            erhoeheWertFuerHeutigesDatum( id);
        }
            }
    catch(Exception e){

            System.out.println("IGNORE "+matcher.group(1).substring(0,matcher.group(1).length()-1)+" BECAUSE: "+e.getMessage());
    }
    }

    public void UpdatePerformanceAndViews(Matcher matcher) {
        try{
            long id =postRepository.getIdByName(matcher.group(1).substring(0,matcher.group(1).length()-1));
            checkTheTag(id,false);
            if (statsRepo.existsByArtId(id)){
            long views = statsRepo.getClicksByArtId(id);
            views ++;
                LocalDateTime PostTimestamp = postRepository.getPostDateById(id);
                LocalDateTime Now =  LocalDateTime.now();
                Duration duration = Duration.between(PostTimestamp, Now);
                long diffInDays = duration.toDays();
                float Performance = views;
                if (diffInDays>0&&views > 0){
                    Performance = (float)views/diffInDays;
                }

                statsRepo.updateClicksAndPerformanceByArtId(views,id,Performance);
                //updateDailyClicks(id);
                erhoeheWertFuerHeutigesDatum( id);
        }else{  statsRepo.save(new PostStats(id,(float) 0,(float) 0,1,0,0,(float) 0));//updateDailyClicks(id);
                erhoeheWertFuerHeutigesDatum( id);}
            }
    catch(Exception e){
            System.out.println("IGNORE "+matcher.group(1).substring(0,matcher.group(1).length()-1)+" BECAUSE: "+e.getMessage());
       // e.printStackTrace();
    }
    }

    @Transactional
    public void updateUserStats(WPUser user){
        if(userStatsRepo.existsByUserId(user.getId())){
            UserStats Stats = userStatsRepo.findByUserId(user.getId());
            long views = Stats.getProfileView() + 1 ;
            Stats.setProfileView(views);
            List<Post> list = postRepository.findByAuthor(user.getId().intValue());
            int count = 0;
            float relevance=0;
            float performance=0;
            for(Post p:list){
                if(statsRepo.existsByArtId(p.getId())){
                    PostStats PostStats = statsRepo.getStatByArtID(p.getId());
                    count ++;
                    relevance=relevance+PostStats.getRelevance();
                    performance=performance+PostStats.getPerformance();
                }
            }
            if(count !=0){
            relevance=relevance/count;
            performance=performance/count;
            Stats.setAveragePerformance(performance);
            Stats.setAverageRelevance(relevance);}
            userStatsRepo.save(Stats);


        }else{userStatsRepo.save(new UserStats(user.getId(), (float) 0,(float) 0, 0,(float) 0,(float) 0,(float)0,(long)0));}
    }
    @Transactional
    public void updateUserStatsForAllUsers() {
        List<WPUser> allUsers = wpUserRepo.findAll();

        for (WPUser user : allUsers) {
            if (userStatsRepo.existsByUserId(user.getId())) {
                UserStats stats = userStatsRepo.findByUserId(user.getId());
                long views = stats.getProfileView() + 1;
                stats.setProfileView(views);

                List<Post> posts = postRepository.findByAuthor(user.getId().intValue());
                int count = 0;
                float relevance = 0;
                float performance = 0;

                for (Post post : posts) {
                    if (statsRepo.existsByArtId(post.getId())) {
                        PostStats postStats = statsRepo.getStatByArtID(post.getId());
                        count++;
                        relevance += postStats.getRelevance();
                        performance += postStats.getPerformance();
                    }
                }

                if (count != 0) {
                    relevance = relevance / count;
                    performance = performance / count;
                    stats.setAveragePerformance(performance);
                    stats.setAverageRelevance(relevance);
                }

                userStatsRepo.save(stats);
            } else {
                userStatsRepo.save(new UserStats(user.getId(), (float) 0,(float) 0, 0,(float) 0,(float) 0,(float)0,(long)0));
            }
        }
    }

    @Transactional
    public void updateDailyClicks(long id){
        PostStats PostStats = statsRepo.getStatByArtID(id);
        HashMap<String,Long> daily = (HashMap<String, Long>) PostStats.getViewsLastYear();
        Calendar calendar = Calendar.getInstance();
        int currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        long views = daily.get(Integer.toString(currentDayOfYear));
        views++;
        daily.put(Integer.toString(currentDayOfYear),views);
        PostStats.setViewsLastYear((Map<String,Long>) daily);
        PostStats.setRelevance(getRelevance(daily,currentDayOfYear,7));
        statsRepo.save(PostStats);

    }

    @Transactional
    public void erhoeheWertFuerHeutigesDatum(long id) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
        // I'm assuming PostStats is a class, first letter should be lowercase for the instance
        PostStats postStats = statsRepo.findByArtIdAndAndYear(id,aktuellesJahr);
        HashMap<String, Long> daily = (HashMap<String, Long>) postStats.getViewsLastYear();

        // Das heutige Datum im Format dd.MM abrufen
        String heutigesDatum = LocalDate.now().format(formatter);

        // Den Wert für das heutige Datum in der HashMap um 1 erhöhen
        long aktuellerWert = daily.getOrDefault(heutigesDatum, 0L);
        daily.put(heutigesDatum, aktuellerWert + 1);
        postStats.setViewsPerHour(erhoeheViewsPerHour(postStats));
        postStats.setViewsLastYear(daily);
        postStats.setRelevance(getRelevance2(daily, heutigesDatum, 7));

        statsRepo.save(postStats);
    }

    @Transactional
    public Map<String,Long> erhoeheViewsPerHour(PostStats stats){
        Map<String,Long> viewsPerHour =stats.getViewsPerHour();
        LocalTime jetzt = LocalTime.now();
        int stunde = jetzt.getHour();
        if(stunde != 0){stunde--;}
        long views= viewsPerHour.getOrDefault(Integer.toString(stunde),0L);
        views++;
        viewsPerHour.put(Integer.toString(stunde),views);
        stats.setViewsPerHour(viewsPerHour);

        return viewsPerHour;
    }
    public static float getRelevance2(HashMap<String, Long> viewsLastYear, String currentDateString, int time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-dd.MM");

        // Add the current year to the date string
        String year = String.valueOf(LocalDate.now().getYear());
        LocalDate currentDate = LocalDate.parse(year + "-" + currentDateString, formatter);

        long views = 0;

        for (int i = 0; i < time; i++) {
            String dateKey = currentDate.minusDays(i).format(DateTimeFormatter.ofPattern("dd.MM"));
            views += viewsLastYear.getOrDefault(dateKey, 0L);
        }

        return (float) views / time;
    }



    public float getRelevance(HashMap<String,Long>viewsLastYear,int currentDayOfYear,int time){
        int counter =currentDayOfYear-time;
        long views=0;
        while(counter<=currentDayOfYear){
            views=views+(viewsLastYear.get(Integer.toString(counter)));
            counter++;
        }
        return (float)views/time;
    }

    @Transactional
    public void updateTagStats(long id,boolean searchSuccess){
        TagStat Stats = tagStatRepo.getStatById((int)id);
        HashMap<String,Long> daily = (HashMap<String, Long>) Stats.getViewsLastYear();
        Calendar calendar = Calendar.getInstance();
        int currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        long views = daily.get(Integer.toString(currentDayOfYear));
        views++;
        daily.put(Integer.toString(currentDayOfYear),views);
        Stats.setViewsLastYear((Map<String,Long>) daily);
        views = Stats.getViews();
        views ++;
        Stats.setViews(views);
        Stats.setRelevance(getRelevance(daily,currentDayOfYear,7));
        if(searchSuccess){
            int searchS = Stats.getSearchSuccess();
            searchS++;
            Stats.setSearchSuccess(searchS);
        }
        tagStatRepo.save(Stats);
    }

    public void checkTheTag(long id,boolean searchSuccess){
        List<Long> tagTaxIds= termRelRepo.getTaxIdByObject(id);
        List<Long> tagIds= termTaxRepo.getTermIdByTaxId(tagTaxIds);
        for(Long l:tagIds){
            if(tagStatRepo.existsByTagId(l.intValue())){
                updateTagStats(l.intValue(),searchSuccess);}
            else{ tagStatRepo.save(new TagStat(l.intValue(),0,0,(float)0,(float)0));
                updateTagStats(l.intValue(),searchSuccess);}
    }}

    public void updateUserActivity(Long period){
        List<WPUser> users = wpUserRepo.findAll();
        List<Post> posts= new ArrayList<>();
        UserStats stats = null ;
        float postfreq = 0 ;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime postTime= now.minusMonths(period);
        long daysDifference = ChronoUnit.DAYS.between(postTime, now);
        int counter =0;
        for(WPUser user: users){
            posts=postRepository.findByAuthor(user.getId().intValue());
            counter = 0 ;
            if(!posts.isEmpty()){
            for (Post post:posts){
                if(postTime.isBefore(post.getDate())&& post.getStatus().equals("publish") && post.getType().equals("post")){counter ++;}
            }
            if(counter!=0){
            postfreq=(float)daysDifference/counter;
            }}else{postfreq=0;}
            if (userStatsRepo.existsByUserId(user.getId())){
                stats = userStatsRepo.findByUserId(user.getId());
            }else{stats = new UserStats(user.getId(), (float) 0,(float) 0, 1,(float) 0,(float) 0,(float)0,(long)0);}
            stats.setPostFrequence(postfreq);
            userStatsRepo.save(stats);
            updateInteractionRate(user,stats,posts);
        }

    }

    public void updateInteractionRate(WPUser user,UserStats stats, List<Post>posts){
        int commentCount=0;
        int answeredComments=0;
        float interactionRate=0;
        List<Comments> comments = new ArrayList<>();
        for(Post post:posts){
            if(post.getStatus().equals("publish") && post.getType().equals("post")){
                comments=commentRepo.findByPostId(post.getId());
                for(Comments comment:comments){
                    if (comment.getUserId() == user.getId()) {
                        if (commentRepo.findByCommentId(comment.getParentCommentId()).getUserId() != user.getId()) {
                            answeredComments++;
                        }
                    } else {
                        commentCount++;
                    }
                }
            }

        }
        if(answeredComments!=0){
        interactionRate=(float)answeredComments/commentCount;}
        stats.setInteractionRate(interactionRate);
        userStatsRepo.save(stats);
       // System.out.println("Interaktionsrate: "+interactionRate+" id: "+user.getId());
    }


}








