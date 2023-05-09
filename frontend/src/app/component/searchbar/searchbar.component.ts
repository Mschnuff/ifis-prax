import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {CookieService} from "ngx-cookie-service";
import {Tag} from "../../tag/Tag";
import {DbObject} from "../../services/DbObject";
import {DbService} from "../../services/db.service";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";


@Component({
  selector: 'dash-searchbar',
  templateUrl: './searchbar.component.html',
  styleUrls: ['./searchbar.component.css']
})
export class SearchbarComponent implements OnInit{

  @Output() searchInput :string = "";
  @Output() currentSearch = new EventEmitter<string>();
  @Output() selected = new EventEmitter<DbObject>();

  @Input() page : string = "placeholder";

  selectedSearch : DbObject = {id: "0", name: ""};
  displaySearchBox: string = "";
  imgSrc: SafeUrl = "";

  constructor(private cookieService : CookieService, private db : DbService) {
  }

  ngOnInit(): void {
    let object :string[]  = ["0",""];
    if(this.cookieService.check(this.page)){
      object = this.cookieService.get(this.page).split(":");
    }

    this.onDbObjectSelected(object[0], object[1]);
  }

  onKey(value : string) {
    this.searchInput = value;
    this.currentSearch.emit(value);
  }

  onDbObjectSelected(id: string, name: string){
    let object : DbObject = {id, name};

    if (id != "0"){
      this.selectedSearch = object;
      this.displaySearchBox = "0";
    }
    this.selected.emit({id, name});
    this.cookieService.set(this.page, object.id + ":" + object.name, {expires : 7});

    this.getImgSrc(this.selectedSearch.id);
  }

  onCancelClick(){
    this.selectedSearch = {id: "0", name: ""};
    this.displaySearchBox = "50px";
    this.onKey("");
    this.onDbObjectSelected("0", "");
  }

  getImgSrc(id: string) {
    this.db.getUserImgSrc(id).then(dataUrl => {
      this.imgSrc = dataUrl;
    });
  }
}
