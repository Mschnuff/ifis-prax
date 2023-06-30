package com.analysetool.api;



import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.analysetool.modells.*;
import com.analysetool.repositories.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
//@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private statsRepository statRepository;
    @Autowired
    private PostRepository postRepo;
    @Autowired
    private UserStatsRepository userStatsRepo;
    @Autowired
    private TagStatRepository tagStatRepo;
    @Autowired
    private WPTermRepository termRepo;
    @Autowired
    private WpTermRelationshipsRepository termRelRepo;
    @Autowired
    private WpTermTaxonomyRepository taxTermRepo;
    @Autowired
    private WPUserRepository userRepo;
    PostRepository postRepository;
    statsRepository statsRepo;
    WpTermRelationshipsRepository termRelationRepo;
    WPTermRepository wpTermRepo;
    WpTermTaxonomyRepository wpTermTaxonomyRepo;

    @Autowired
    public PostController(
            PostRepository postRepository, statsRepository statsRepo, WpTermRelationshipsRepository termRelationRepo, WPTermRepository wpTermRepo, WpTermTaxonomyRepository wpTermTaxonomyRepo
    ){
       this.postRepository = postRepository;
       this.statsRepo=statsRepo;
       this.termRelationRepo = termRelationRepo;
       this.wpTermRepo = wpTermRepo;
       this.wpTermTaxonomyRepo = wpTermTaxonomyRepo;
    }

    @GetMapping("/posts/getall")
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    @GetMapping("/posts/publishedPosts")
    public List<Post> getPublishedPosts(){return postRepository.findPublishedPosts();}


/*    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable("id") Long id) {
        Optional<Post> postData = postRepository.findById(id);

        if (postData.isPresent()) {
            return new ResponseEntity<>(postData.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }*/
    /*


        @GetMapping("/getPostsByAuthorLine")
        public String PostsByAuthor(@RequestParam int id) throws JSONException, ParseException {

            JSONArray list = new JSONArray();
            List<Post> posts = postRepository.findByAuthor(id);
            DateFormat onlyDate = new SimpleDateFormat("dd/MM/yyyy");
            if(!posts.isEmpty()){
                for(Post i:posts) {

                    JSONObject obj = new JSONObject();
                    Date Tag = onlyDate.parse(i.getDate().toString());

                    if ( (!list.isNull(list.length()-1))   &&  (list.getJSONObject(list.length()-1).get("date") == Tag ))
                        { list.getJSONObject(list.length()-1).put("id",list.getJSONObject(list.length()-1).get("id")+","+i.getTitle()) ;}

                    else{
                    obj.put("id", i.getTitle());
                        obj.put("date", Tag);
                        list.put(obj);}
                }
            }
            return list.toString();
        }*/
@GetMapping("/getPostsByAuthorLine")
public String PostsByAuthor(@RequestParam int id) throws JSONException, ParseException {

    JSONArray list = new JSONArray();
    List<Post> posts = postRepository.findByAuthor(id);
    DateFormat onlyDate = new SimpleDateFormat("yyyy-MM-dd");

    if (!posts.isEmpty()) {
        for (Post i : posts) {
            JSONObject obj = new JSONObject();
            Date date = onlyDate.parse(i.getDate().toString());
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(date);

            obj.put("title", i.getTitle());
            obj.put("date", formattedDate);
            obj.put("count",1);

            if (list.length() > 0 && list.getJSONObject(list.length() - 1).getString("date").equals(formattedDate)) {
                String currentId = list.getJSONObject(list.length() - 1).getString("title");
                int currentCount = list.getJSONObject(list.length() - 1).getInt("count");
                list.getJSONObject(list.length() - 1).put("title", currentId + "," + i.getTitle());
                list.getJSONObject(list.length() - 1).put("count", currentCount + 1);
            } else {
                list.put(obj);
            }
        }
    }
    return list.toString();
}

    @GetMapping("/getPostsByAuthorLine2")
    public String PostsByAuthor2(@RequestParam int id) throws JSONException, ParseException {

        JSONArray list = new JSONArray();
        List<Post> posts = postRepository.findByAuthor(id);
        DateFormat onlyDate = new SimpleDateFormat("yyyy-MM-dd");

        String type = "";

        if (!posts.isEmpty()) {
            for (Post i : posts) {
                if (i.getType().equals("post")) {
                    stats Stats = null;
                    if (statsRepo.existsByArtId(i.getId())) {
                        Stats = statsRepo.getStatByArtID(i.getId());
                    }
                    List<Long> tagIDs = null;
                    if (termRelationRepo.existsByObjectId(i.getId())) {
                        tagIDs = termRelationRepo.getTaxIdByObject(i.getId());
                    }
                    List<WPTerm> terms = new ArrayList<>();
                    if (tagIDs != null) {
                        for (long l : tagIDs) {
                            if (wpTermRepo.existsById(l)) {
                                if (wpTermRepo.findById(l).isPresent()) {
                                    terms.add(wpTermRepo.findById(l).get());
                                }
                            }
                        }
                    }
                    for (WPTerm t : terms) {
                        if (wpTermTaxonomyRepo.existsById(t.getId())) {
                            if (wpTermTaxonomyRepo.findById(t.getId()).isPresent()) {
                                WpTermTaxonomy tt = wpTermTaxonomyRepo.findById(t.getId()).get();
                                if (Objects.equals(tt.getTaxonomy(), "category") && tt.getTermId() != 1) {
                                    if (wpTermRepo.findById(tt.getTermId()).isPresent()) {
                                        type = wpTermRepo.findById(tt.getTermId()).get().getSlug();
                                        switch (type) {
                                            case "artikel":
                                                break;
                                            case "blog":
                                                break;
                                            case "pressemitteilung":
                                                break;
                                            default:
                                                type = "default";
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    JSONObject obj = new JSONObject();
                    Date date = onlyDate.parse(i.getDate().toString());
                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(date);

                    obj.put("id", i.getId());
                    obj.put("title", i.getTitle());
                    obj.put("date", formattedDate);
                    obj.put("type", type);
                    if (Stats != null) {
                        obj.put("performance", Stats.getPerformance());
                        obj.put("relevance", Stats.getRelevance());
                    } else {
                        obj.put("performance", 0);
                        obj.put("relevance", 0);
                    }


               /* if (list.length() > 0 && list.getJSONObject(list.length() - 1).getString("date").equals(formattedDate)) {
                    String currentId = list.getJSONObject(list.length() - 1).getString("title");
                   // double currentCount = list.getJSONObject(list.length() - 1).getDouble("performance");
                    list.getJSONObject(list.length() - 1).put("title", currentId + "," + i.getTitle());
                    //list.getJSONObject(list.length() - 1).put("performance", currentCount + 1);
                } else {
                    list.put(obj);
                }*/
                    if (!obj.get("type").equals("default")) {
                        list.put(obj);
                    }
                }
            }
        }
        return list.toString();
    }

    @GetMapping("/getNewestPostWithStatsByAuthor")
    public String getNewestPostWithStatsByAuthor(@RequestParam Long id) throws JSONException, ParseException {
        List<Post> posts = postRepository.findByAuthor(id.intValue());
        long newestId = 0;
        LocalDateTime newestTime = null;
        for (Post post : posts) {
            if (newestTime == null || newestTime.isBefore(post.getDate())) {
                if (post.getType().equals("post")){
                    newestTime = post.getDate();
                    newestId = post.getId();
                }
            }
        }
        return PostsById2(newestId);
    }

    @GetMapping("/getPostWithStatsById")
    public String PostsById2(@RequestParam long id) throws JSONException, ParseException {
        if(!postRepository.findById(id).isPresent()) {return null;}
        Post post = postRepository.findById(id).get();
        List<String> tags = new ArrayList<>();
        String type = "default";

        stats Stats = null;
        if(statsRepo.existsByArtId(post.getId())){
            Stats = statsRepo.getStatByArtID(post.getId());
        }
        List<Long> tagIDs = null;
        if(termRelationRepo.existsByObjectId(post.getId())){
            tagIDs = termRelationRepo.getTaxIdByObject(post.getId());
        }
        List<WPTerm> terms = new ArrayList<>();
        if (tagIDs != null) {
            for (long l : tagIDs) {
                if (wpTermRepo.existsById(l)) {
                    if (wpTermRepo.findById(l).isPresent()) {
                        terms.add(wpTermRepo.findById(l).get());
                    }
                }
            }
        }
        for (WPTerm t: terms) {
            if (wpTermTaxonomyRepo.existsById(t.getId())){
                if (wpTermTaxonomyRepo.findById(t.getId()).isPresent()){
                    WpTermTaxonomy tt = wpTermTaxonomyRepo.findById(t.getId()).get();
                    if (Objects.equals(tt.getTaxonomy(), "category")){
                        if (wpTermRepo.findById(tt.getTermId()).isPresent() && tt.getTermId() != 1) {
                            type = wpTermRepo.findById(tt.getTermId()).get().getSlug();
                        }
                    } else if (Objects.equals(tt.getTaxonomy(), "post_tag")) {
                        tags.add(wpTermRepo.findById(tt.getTermId()).get().getName());
                    }
                }
            }
        }

        JSONObject obj = new JSONObject();
        DateFormat onlyDate = new SimpleDateFormat("yyyy-MM-dd");
        Date date = onlyDate.parse(post.getDate().toString());
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(date);

        obj.put("id", post.getId());
        obj.put("title", post.getTitle());
        obj.put("date", formattedDate);
        obj.put("tags", tags);
        obj.put("type", type);
        if(Stats != null){
            obj.put("performance",Stats.getPerformance());
            obj.put("relevance",Stats.getRelevance());
            obj.put("clicks", Stats.getClicks().toString());
            obj.put("searchSuccesses",Stats.getSearchSuccess());
            obj.put("searchSuccessRate",Stats.getSearchSuccessRate());
            obj.put("referrings",Stats.getRefferings());
            obj.put("articleReferringRate",Stats.getArticleReferringRate());
        }else {
            obj.put("performance",0);
            obj.put("relevance",0);
            obj.put("clicks", "0");
            obj.put("searchSuccesses",0);
            obj.put("searchSuccessRate",0);
            obj.put("referrings",0);
            obj.put("articleReferringRate",0);}

        return obj.toString();
    }

    //STATS

   /* @GetMapping("/{id}")
    public Optional<stats> getStat(@PathVariable Long id) {
        return statRepository.findById(id);
    }*/

    @GetMapping
    public List<stats> getAllStats() {
        return statRepository.findAll();
    }

    @GetMapping("/maxPerformance")
    public float getMaxPerformance(){
        return statRepository.getMaxPerformance();
    }

    @GetMapping("/maxRelevance")
    public float getMaxRelevance(){
        return statRepository.getMaxRelevance();
    }
    @GetMapping("/getPerformanceByArtId")
    public float getPerformanceByArtId(@RequestParam int id){
        return statRepository.getPerformanceByArtID(id);
    }

    @GetMapping("/getViewsOfUser")
    public long getViewsOfUserById(@RequestParam Long id){
        List<Post> posts = postRepo.findByAuthor(id.intValue());
        long views = 0 ;
        int tagIdBlog = termRepo.findBySlug("blog").getId().intValue();
        int tagIdArtikel = termRepo.findBySlug("artikel").getId().intValue();

        int tagIdPresse = termRepo.findBySlug("pressemitteilung").getId().intValue();
        for (Post post : posts) {
            if (statRepository.existsByArtId(post.getId())) {
                stats Stat = statRepository.getStatByArtID(post.getId());
                for (Long l : termRelRepo.getTaxIdByObject(post.getId())) {
                    for (WpTermTaxonomy termTax : taxTermRepo.findByTermTaxonomyId(l)) {
                        if (termTax.getTermId() == tagIdBlog||termTax.getTermId() == tagIdArtikel||termTax.getTermId() == tagIdPresse) {
                            views = views + Stat.getClicks();
                        }
                    }


                }
            }
        }
        return views ;
    }

    @GetMapping("/getPostCountOfUser")
    public long getPostCountOfUserById(@RequestParam Long id){
        List<Post> posts = postRepo.findByAuthor(id.intValue());
        long PostCount = 0 ;
        int tagIdBlog = termRepo.findBySlug("blog").getId().intValue();
        int tagIdArtikel = termRepo.findBySlug("artikel").getId().intValue();

        int tagIdPresse = termRepo.findBySlug("pressemitteilung").getId().intValue();
        for (Post post : posts) {
            if (statRepository.existsByArtId(post.getId())) {
                stats Stat = statRepository.getStatByArtID(post.getId());
                for (Long l : termRelRepo.getTaxIdByObject(post.getId())) {
                    for (WpTermTaxonomy termTax : taxTermRepo.findByTermTaxonomyId(l)) {
                        if (termTax.getTermId() == tagIdBlog||termTax.getTermId() == tagIdArtikel||termTax.getTermId() == tagIdPresse) {
                            PostCount++ ;
                        }
                    }


                }
            }
        }
        return PostCount ;
    }

    @GetMapping("/getViewsBrokenDown")
    public String getViewsBrokenDown(@RequestParam Long id) throws JSONException {
        long viewsBlog = 0;
        long viewsArtikel = 0;
        long viewsProfile = userStatsRepo.findByUserId(id).getProfileView();
        int tagIdBlog = termRepo.findBySlug("blog").getId().intValue();
        int tagIdArtikel = termRepo.findBySlug("artikel").getId().intValue();

        int tagIdPresse = termRepo.findBySlug("pressemitteilung").getId().intValue();
        long viewsPresse = 0;
        List<Post> posts = postRepo.findByAuthor(id.intValue());

        List<Long> postTags = new ArrayList<>();
        for (Post post : posts) {
            if (statRepository.existsByArtId(post.getId())) {
                stats Stat = statRepository.getStatByArtID(post.getId());
                for (Long l : termRelRepo.getTaxIdByObject(post.getId())) {
                    for (WpTermTaxonomy termTax : taxTermRepo.findByTermTaxonomyId(l)) {
                        if (termTax.getTermId() == tagIdBlog) {
                            viewsBlog = viewsBlog + Stat.getClicks();
                        }
                        if (termTax.getTermId() == tagIdArtikel) {
                            viewsArtikel = viewsArtikel + Stat.getClicks();
                        }
                        if (termTax.getTermId() == tagIdPresse) {
                            viewsPresse = viewsPresse + Stat.getClicks();
                        }}


                }
            }
        }
        JSONObject obj = new JSONObject();
        obj.put("viewsBlog", viewsBlog);
        obj.put("viewsArtikel", viewsArtikel);
        obj.put("viewsPresse", viewsPresse);
        obj.put("viewsProfile", viewsProfile);
        return obj.toString();

    }


    @GetMapping("/bestPost")
    public String getBestPost(@RequestParam Long id, @RequestParam String type) throws JSONException {
        List<Post> Posts = postRepo.findByAuthor(id.intValue());
        if (Posts.size() == 0) {
            return null;
        }
        stats Stats = null;
        float max = 0;
        long PostId = 0;
        for (Post post : Posts) {
            if (statRepository.existsByArtId(post.getId())) {
                Stats = statRepository.getStatByArtID(post.getId());
                if (type.equals("relevance")) {
                    if (Stats.getRelevance() > max) {
                        max = Stats.getRelevance();
                        PostId = Stats.getArtId();
                    }
                }
                if (type.equals("performance")) {
                    if (Stats.getPerformance() > max) {
                        max = Stats.getPerformance();
                        PostId = Stats.getArtId();
                    }
                }
            }
        }

        JSONObject obj = new JSONObject();
        obj.put("ID", PostId);
        obj.put(type, max);
        obj.put("title", postRepo.findById(PostId).get().getTitle());
        return obj.toString();
    }

    @GetMapping("/getPostStat")
    public String getStat2(@RequestParam Long id) throws JSONException {
        stats Stat = statRepository.getStatByArtID(id);
        JSONObject obj = new JSONObject();
        obj.put("Post-Id",Stat.getArtId());
        obj.put("Relevanz",Stat.getRelevance());
        obj.put("Performance",Stat.getPerformance());
        obj.put("Views",Stat.getClicks());
        obj.put("Refferings",Stat.getReferrings());
        obj.put("Article Reffering Rate",Stat.getArticleReferringRate());
        obj.put("Search Successes",Stat.getSearchSucces());
        obj.put("Search Success Rate",Stat.getSearchSuccessRate());

        return obj.toString();
    }


    @GetMapping("/getNewestStatsByAuthor")
    public String getNewestStatsByAuthor(@RequestParam Long id) throws JSONException{
        List<Post> posts =postRepo.findByAuthor(id.intValue()) ;
        long newestId = 0 ;
        LocalDateTime newestTime = null ;
        for(Post post : posts){
            if(newestTime == null || newestTime.isBefore(post.getDate())){
                newestTime = post.getDate();
                newestId = post.getId();
            }
        }
        long views = 0;
        long searchSuccesses = 0;
        float SearchSuccessRate = 0 ;
        long refferings = 0;
        float refrate = 0;
        float relevanz = 0;
        float performance = 0 ;

        if(statRepository.existsByArtId(newestId)){
            stats Stats = statRepository.getStatByArtID(newestId);
            views = Stats.getClicks();
            searchSuccesses = Stats.getSearchSuccess();
            SearchSuccessRate = Stats.getSearchSuccessRate();
            refferings = Stats.getRefferings();
            refrate = Stats.getArticleReferringRate();
            relevanz = Stats.getRelevance();
            performance = Stats.getPerformance();
        }
        JSONObject obj = new JSONObject();
        obj.put("ID",newestId);
        obj.put("views",views);
        obj.put("Search Successes",searchSuccesses);
        obj.put("Search Success Rate",SearchSuccessRate);
        obj.put("refferings",refferings);
        obj.put("article reffering rate",refrate);
        obj.put("relevanz",relevanz);
        obj.put("performance",performance);


        return obj.toString();

    }

    @GetMapping("/getNewestStatsByAuthorSessionId")
    public String getNewestStatsByAuthorSessionId(@RequestParam String SessionId) throws JSONException{
        if(userRepo.existsByActivationKey(SessionId)){
            Long id = userRepo.findByActivationKey(SessionId).get().getId();
            List<Post> posts =postRepo.findByAuthor(id.intValue()) ;
            long newestId = 0 ;
            LocalDateTime newestTime = null ;
            for(Post post : posts){
                if(newestTime == null || newestTime.isBefore(post.getDate())){
                    newestTime = post.getDate();
                    newestId = post.getId();
                }
            }
            long views = 0;
            long searchSuccesses = 0;
            float SearchSuccessRate = 0 ;
            long refferings = 0;
            float refrate = 0;
            float relevanz = 0;
            float performance = 0 ;

            if(statRepository.existsByArtId(newestId)){
                stats Stats = statRepository.getStatByArtID(newestId);
                views = Stats.getClicks();
                searchSuccesses = Stats.getSearchSuccess();
                SearchSuccessRate = Stats.getSearchSuccessRate();
                refferings = Stats.getRefferings();
                refrate = Stats.getArticleReferringRate();
                relevanz = Stats.getRelevance();
                performance = Stats.getPerformance();
            }
            JSONObject obj = new JSONObject();
            obj.put("ID",newestId);
            obj.put("views",views);
            obj.put("Search Successes",searchSuccesses);
            obj.put("Search Success Rate",SearchSuccessRate);
            obj.put("refferings",refferings);
            obj.put("article reffering rate",refrate);
            obj.put("relevanz",relevanz);
            obj.put("performance",performance);


            return obj.toString();}
        else{return "SESSION ID WRONG";}

    }


}

