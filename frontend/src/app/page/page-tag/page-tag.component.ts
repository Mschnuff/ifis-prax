import {Component, OnInit} from '@angular/core';
import {CookieService} from "ngx-cookie-service";

@Component({
  selector: 'dash-page-tag',
  templateUrl: './page-tag.component.html',
  styleUrls: ['./page-tag.component.css']
})
export class PageTagComponent implements OnInit{
  displayContent: string = "none";

  constructor(private cookieService : CookieService) {
  }
  onSelected(tag : string){
    if (tag != ""){
      this.displayContent = "grid";
    } else {
      this.displayContent = "none";
    }
  }

  ngOnInit(): void {
    this.onSelected(this.cookieService.get("tag"));
  }

}
