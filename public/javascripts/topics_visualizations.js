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

  var circle = svg.selectAll("circle")
    .data(nodes)
    .enter()
    .append("circle")
    .attr("class", function(d) { return d.parent ? d.children ? "node" : "node node--leaf" : "node node--root"; })
    .style("fill", function(d) { return d.children ? color(d.depth) : null; })
    .each(function(d){
      d3.selectAll(".node--leaf").on("click", function(d){ console.log("hep");});
    })
    .on("click", function(d) {
      if (focus !== d) {
        d3.event.stopPropagation();
        if ( d.children !== undefined) {
          <!--Clicked on parent topic so zoom there-->
          zoom(d);
        } else if (focus !== d.parent){
          <!--Clicked on subtopic from outside, so zoom to parent-->
          zoom(d.parent);
        } else {
          <!--Clicked on subtopic from within parent, so forward to subtopic-->
          window.location.href = d.url;
        }
      }







      // console.log( "var k:" + diameter / (root.r * 2 + margin) ) ;
      console.log( "circle radius:" + d.r * diameter / (root.r * 2 + margin) );
      var radius = d.r * diameter / (root.r * 2 + margin);






    });

  var text = svg.selectAll("text")
    .data(nodes)
    .enter()
    .append("text")
    .attr("class", "label")
    .style("fill-opacity", function(d) { return d.parent === root ? 1 : 0; })
    .style("display", function(d) { return d.parent === root ? null : "none"; })
    .style("font-size", "90%")
    .text(function(d) { return d.name; });

  var node = svg.selectAll("circle,text");

  zoomTo([root.x, root.y, root.r * 2 + margin]);

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
      .style("fill-opacity", function(d) { return d.parent === focus ? 1 : 0; })
      .each("start", function(d) { if (d.parent === focus) this.style.display = "inline"; })
      .each("end", function(d) { if (d.parent !== focus) this.style.display = "none"; });
  }

  function zoomTo(v) {
    var k = diameter / v[2]; view = v;
    node.attr("transform", function(d) { return "translate(" + (d.x - v[0]) * k + "," + (d.y - v[1]) * k + ")"; });
    circle.attr("r", function(d) { return d.r * k; });
  }
});

d3.select(self.frameElement).style("height", diameter + "px");
// End bubble chart
