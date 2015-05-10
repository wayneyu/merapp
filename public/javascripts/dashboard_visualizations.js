//Begin: Donut charts for exams in progress
var width = 260,
height = 250,
circle_width = 35;

var color = ["#5cb85c", "#d9534f", "#f0ad4e", "#5bc0de"];

var pie = d3.layout.pie()
    .sort(null);

var arc = d3.svg.arc();

d3.json("/dashboard/exams_in_progress/flags_per_exam", function(error, dataset) {
if (error) return console.error(error);

for (var i = 0; i < dataset.length; ++i) {
    var svg = d3.select("#exams_in_progress").append("svg")
        .attr("width", width)
        .attr("height", height)
        .append("g")
        .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");


    var text = svg.append("text")
        .attr("x", 0)
        .attr("y", -5 -3*circle_width)

    text.append("a")
        .attr("xlink:href", dataset[i].url)
        .style("text-anchor", "middle")
        .style("font-size", 16)
        .text(dataset[i].exam);


//    Inner circle: Statements
    var path = svg.selectAll("path0")
        .data(pie(dataset[i].Statements))
        .enter().append("path")
        .attr("fill", function(d, i) { return color[i]; })
        .attr("d", arc.innerRadius(10 + 0*circle_width).outerRadius(circle_width*(0+1)) );

    var text = svg.append("text")
        .attr("x", 0)
        .attr("y", 0)
        .style("text-anchor", "middle")
        .text("Statements")

//    Middle circle: Hints
    var path = svg.selectAll("path1")
        .data(pie(dataset[i].Hints))
        .enter().append("path")
        .attr("fill", function(d, i) { return color[i]; })
        .attr("d", arc.innerRadius(10 + 1*circle_width).outerRadius(circle_width*(1+1)) );

    var text = svg.append("text")
        .attr("x", 0)
        .attr("y", 20 - 2*circle_width)
        .style("text-anchor", "middle")
        .text("Hints")

//    Outer circle: Solutions
    var path = svg.selectAll("path2")
        .data(pie(dataset[i].Solutions))
        .enter().append("path")
        .attr("fill", function(d, i) { return color[i]; })
        .attr("d", arc.innerRadius(10 + 2*circle_width).outerRadius(circle_width*(2+1)) );

    var text = svg.append("text")
        .attr("x", 0)
        .attr("y", 20 - 3*circle_width)
        .style("text-anchor", "middle")
        .text("Solutions")


//    Finally, add progress percentage
    var text = svg.append("text")
        .attr("x", 0)
        .attr("y", -6 + 2*circle_width)
        .style("text-anchor", "middle")
        .style("font-size", 24)
        .text(dataset[i].progress + "%");

}
});
//End: Donut charts for exams in progress