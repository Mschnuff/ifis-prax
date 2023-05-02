import {AfterViewInit, Component, Input, OnInit} from '@angular/core';
import {Chart, ChartType} from "chart.js/auto";

@Component({
  selector: 'dash-podium',
  templateUrl: './podium.component.html',
  styleUrls: ['./podium.component.css']
})
export class PodiumComponent implements OnInit{

  @Input() desc : string = "";
  @Input() winners : string[] = ["", "", ""];
  winnersShort : string[] = ["", "", ""]

  ngOnInit(): void {
    this.winnersShort[0] = this.winners[0].slice(0, 11);
    this.winnersShort[1] = this.winners[1].slice(0, 11);
    this.winnersShort[2] = this.winners[2].slice(0, 11);
  }
}