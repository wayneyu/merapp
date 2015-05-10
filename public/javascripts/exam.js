// Calculate exam-average easiness
var total_easiness = 0;
var num_rated_questions = 0;
var total_ratings = 0;
$(".question_easiness").each(function(){
    var val = parseFloat($(this).text().replace("%",""));
    if(!isNaN(val)){
        total_easiness += val;
        num_rated_questions += 1;
        total_ratings += parseFloat($(this).attr("title"));
    }
});
$(".exam_easiness").text(Math.round(10*total_easiness/num_rated_questions)/10 + "/5");
$(".rating_count").text(total_ratings);

// Quality flags to squares
function background_from_flag(flag) {
    if (flag.indexOf("C") >= 0) {return "#5bc0de";}
    else if (flag.indexOf("R") >= 0) {return "#f0ad4e";}
    else if (flag.indexOf("QB") >= 0) {return "#d9534f";}
    else {return "#5cb85c";}
}
$(".quality_flag_question").each(function(){
    $(this).css("background-color", background_from_flag($(this).text()));
    $(this).text("");
});