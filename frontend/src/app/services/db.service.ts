import {Injectable} from '@angular/core';
import {User} from "../page/page-einzel/user/user.component";
import {DomSanitizer} from "@angular/platform-browser";
import {Tag} from "../page/tag/Tag";
import {DbObject} from "./DbObject";
import {Post} from "../Post";

export enum dbUrl {
  HOST = "http://localhost",
  PORT = ":8080",
  GET_ALL_TAGS = "/tags/getPostTagsIdName",
  GET_ALL_TAGS_WITH_COUNT_AND_RELEVANCE = "/tags/allTermsRelevanceAndCount",
  GET_TAG_POST_COUNT = "/tags/getPostcount?id=",
  GET_TAG_RANKING = "/tags/getTermRanking",

  GET_POSTS_PER_USER_PER_DAY = "/posts/getPostsByAuthorLine?id=",
  GET_POSTS_PER_USER_WITH_STATS = "/posts/getPostsByAuthorLine2?id=",
  GET_POSTS_NEWEST_BY_USER_WITH_STATS = "/posts/getNewestPostWithStatsByAuthor?id=",
  GET_POST_BY_USERS_BEST = "/posts/bestPost?id=",

  GET_USERS_ALL = "/users/getAllNew",
  GET_USER_IMG = "/users/profilePic?id=",
  GET_USER_CLICKS = "/users/getViewsBrokenDown?id=",

  GET_POST = "posts/getPostWithStatsById?id=",
  GET_POST_PERFORMANCE = "/posts/getPerformanceByArtId?id=",
  GET_POST_MAX_PERFORMANCE = "/posts/maxPerformance",
  GET_POST_MAX_RELEVANCE = "/posts/maxRelevance",

  LOGIN = "/login"
}

@Injectable({
  providedIn: 'root'
})
export class DbService {

  private static host = dbUrl.HOST;
  private static port = dbUrl.PORT;

  public static Tags : Tag[] = [];
  public static Users : User[] = [];

  constructor(private sanitizer : DomSanitizer) { }

  private static getUrl( prompt : string){
    return DbService.host + DbService.port + prompt;
  }

  async login(username : string, userpass : string) {
    return await fetch(DbService.getUrl(dbUrl.LOGIN) + "?user=" + username + "&pass=" + userpass).then(res => res.blob());
  }

  async loadAllTags(){
    if (DbService.Tags.length > 0){
      return;
    }
    await fetch(DbService.getUrl(dbUrl.GET_ALL_TAGS)).then(res => res.json()).then(res => {
      for (let tag of res) {
        DbService.Tags.push(tag);
      }
    });
  }
  async getAllTagsWithCountAndRelevance(){
    return await fetch(DbService.getUrl(dbUrl.GET_ALL_TAGS_WITH_COUNT_AND_RELEVANCE)).then(res => res.json());
  }

  async getTagPostCount(id : string){
    return await fetch(DbService.getUrl(dbUrl.GET_TAG_POST_COUNT) + id).then(res => res.json());
  }

  async getTagRanking() {
    return await fetch(DbService.getUrl(dbUrl.GET_TAG_RANKING)).then(res => res.json());
  }

  async loadAllUsers() {
    if (DbService.Users.length > 0){
      return;
    }
    await fetch(DbService.getUrl(dbUrl.GET_USERS_ALL)).then(res => res.json()).then(res => {
      for (let user of res) {
        DbService.Users.push(user);
      }
    });
  }

  async getUserPostsDay(id : string){
    return await fetch(DbService.getUrl(dbUrl.GET_POSTS_PER_USER_PER_DAY) + id).then(res => res.json());
  }
  async getUserPostsWithStats(id : string){
    return await fetch(DbService.getUrl(dbUrl.GET_POSTS_PER_USER_WITH_STATS) + id).then(res => res.json());
  }

  async getUserImgSrc(id : string){
    const blob = await fetch(DbService.getUrl(dbUrl.GET_USER_IMG) + id).then(res => res.blob());
    const reader = new FileReader();
    reader.readAsDataURL(blob);
    return new Promise<string>((resolve, reject) => {
      reader.onloadend = () => {
        if (typeof reader.result === 'string') {
          resolve(reader.result);
        } else {
          reject(new Error('Failed to create data URL from blob'));
        }
      };
      reader.onerror = () => {
        reject(new Error('Failed to read blob data'));
      };
    }).then(dataUrl => this.sanitizer.bypassSecurityTrustUrl(dataUrl));
  }

  async getUserClicks(id : string){
    return fetch(DbService.getUrl(dbUrl.GET_USER_CLICKS + id)).then(res => res.json());
  }

  async getUserBestPost(id: string, type: string){
    return fetch(DbService.getUrl(dbUrl.GET_POST_BY_USERS_BEST) + id + "&type=" + type).then(res => res.json()).catch(reason => {return new Post()});
  }

  async getUserNewestPost(id: string): Promise<Post> {
    return fetch(DbService.getUrl(dbUrl.GET_POSTS_NEWEST_BY_USER_WITH_STATS) + id).then(res => res.json());
  }

  async getMaxPerformance(){
    let max : Promise<number> = await fetch(DbService.getUrl(dbUrl.GET_POST_MAX_PERFORMANCE)).then(res => res.json());
    return max;
  }
  async getMaxRelevance(){
    let max : Promise<number> = await fetch(DbService.getUrl(dbUrl.GET_POST_MAX_RELEVANCE)).then(res => res.json());
    return max;
  }


  static sortAlphanumeric(input : DbObject[]){
    input.sort((a, b) => {
      return a.name.toLowerCase()
        .replace("ü", "ue")
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace(/[\W_]+/g,"")
        .localeCompare(b.name.toLowerCase());
    });
  }

  async getPostById(id: number) : Promise<Post> {
    return await fetch(DbService.getUrl(dbUrl.GET_POST) + id).then(res => res.json());
  }
}
