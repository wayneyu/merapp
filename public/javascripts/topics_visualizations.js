// Start bubble chart
var margin = 10,
  diameter = 560;

var color = d3.scale.linear()
  .domain([-1, 5])
  .range(["hsl(152,80%,80%)", "hsl(228,30%,40%)"])
  .interpolate(d3.interpolateHcl);

var pack = d3.layout.pack()
  .padding(2)
  .size([diameter - margin, diameter - margin])
  .value(function(d) { return d.size; })

var svg = d3.select("#topics_bubble_chart").append("svg")
  .attr("width", diameter)
  .attr("height", diameter)
  .append("g")
  .attr("transform", "translate(" + diameter / 2 + "," + diameter / 2 + ")");

d3.json("/topics/withParents", function(error, root) {
  if (error) return console.error(error);
  root = root[0];
  var focus = root,
    nodes = pack.nodes(root),
    view;

  var focus_on_root = true;

  var handleClick = function(d) {
    if (focus !== d) {
      d3.event.stopPropagation();
      if ( d.children !== undefined) {
        // <!--Clicked on parent topic so zoom there-->
        zoom(d);
      } else if (focus !== d.parent){
        // <!--Clicked on subtopic from outside, so zoom to parent-->
        zoom(d.parent);
      } else {
        // <!--Clicked on subtopic from within parent, so forward to subtopic-->
        window.location.href = d.url;
      }
    }
  }

  var handleFillOpacity = function(d) { //must style after text has been rendered
    var someRadius = d.parent === undefined ? root.r : d.parent.r;
    var radius = d.r * diameter / (someRadius * 2 + margin);
      if (this.getBBox().width>2*radius) {
        return 0.33; //for developement purposes
      }
      else {
        return 1;
      }
  }

  // var handleDisplay = function(d) { //doesnt work, due to conflated logic regarding dislay
  //   var someRadius = d.parent === undefined ? root.r : d.parent.r;
  //   var radius = d.r * diameter / (someRadius * 2 + margin);
  //     if (this.getBBox().width>2*radius) {
  //       return 'none';
  //     }
  //     else {
  //       return 'inline';
  //     }
  // }

  var handleVisibility = function(d) { //doesnt work
    var someRadius = d.parent === undefined ? root.r : d.parent.r;
    var radius = d.r * diameter / (someRadius * 2 + margin);
      if (this.getBBox().width>2*radius) {
        return 'hidden';
      }
      else {
        return 'visible';
      }
  }

  // http://bl.ocks.org/biovisualize/1016860
  var tooltip = d3.select('#topics_bubble_chart')
                  .append("div")
                  .attr("class", "bubble-chart-tooltip")
                  .style("position", "absolute")
                  .style("z-index", "10")
                  .style("visibility", "hidden")
                  .html("") //just initializing
                  ;

  var circle = svg.selectAll("circle")
    .data(nodes)
    .enter()
    .append("circle")
    .attr("class", function(d) { return d.parent ? d.children ? "node" : "node node--leaf" : "node node--root"; })
    .style("fill", function(d) { return d.children ? color(d.depth) : null; })
    .on("click", handleClick)
    // .on('click', function(d) {debugger;})
    .on('mouseover', function(d){
          tooltip.html(d.name);
          tooltip.style("visibility", "visible");
          })
    .on('mouseout', function(d){
          tooltip.style("visibility", "hidden");
          })
    .on("mousemove", function(){
          tooltip.style("top", (event.pageY+8)+"px").style("left",(event.pageX+13)+"px");
          })
    ;

  var text = svg.selectAll("text")
    .data(nodes)
    .enter()
    .append("text")
    .attr("class", "label")
    .text(function(d) { return d.name; })
    .style('cursor','pointer')
    .style("fill-opacity", handleFillOpacity) //redudant/ doesnt do anything, helps in development when handleVisibility is commented out
    .style("display", function(d) { return d.parent === root ? null : "none"; })
    .style("pointer-events", 'none') //transparent to the events, aka pass it to the level below
    // .style("display", handleDisplay) //doesnt work, sdTODO
    .on("click", handleClick)
    ;

  var node = svg.selectAll("circle,text");

  zoomTo([root.x, root.y, root.r * 2 + margin]);

  text.style("visibility", handleVisibility); //firefox can only getBBox() on svg elements _after_ rendering

  function zoom(d) {
    var focus0 = focus; focus = d;

    var transition = d3.transition()
      .duration(d3.event.altKey ? 7500 : 750)
      .tween("zoom", function(d) {
        var i = d3.interpolateZoom(view, [focus.x, focus.y, focus.r * 2 + margin]);
        return function(t) { zoomTo(i(t)); };
      });

    transition.selectAll("text")
      .filter(function(d) { return d.parent === focus || this.style.display === "inline"; })
      .each("start", function(d) { if (d.parent === focus) this.style.display = "inline"; })
      .each("end", function(d) {
        if (d.parent !== focus) this.style.display = "none";
        // http://bost.ocks.org/mike/transition/
        d3.select(this)
          .style("fill-opacity",handleFillOpacity)
          // .style("display", handleDisplay) //doesnt work, sdTODO
          .style("visibility", handleVisibility)
          ;
      });
  }

  function zoomTo(v) {
    var k = diameter / v[2]; view = v;
    node.attr("transform", function(d) { return "translate(" + (d.x - v[0]) * k + "," + (d.y - v[1]) * k + ")"; });
    circle.attr("r", function(d) { return d.r * k; });
  }
});

d3.select(self.frameElement).style("height", diameter + "px");
// End bubble chart
