var sockets = [];
var cpuData;
var ramData;
var charts = [];

for(let i = 0; i < nodeNames.length; i++) {
	let canvasContext = document.getElementById(nodeNames[i] + "-data").getContext('2d');
	let chart = new Chart(canvasContext, {
		type: 'line',
		data: {
			labels: [],
			datasets: [
				{
					label: 'CPU Usage',
					data: [],
					backgroundColor: 'rgba(186, 230, 255, .5)',
					borderColor: 'rgb(119, 207, 255)'
				},
				{
					label: 'RAM Usage',
					data: [],
					backgroundColor: 'rgba(250, 193, 255, .5)',
					borderColor: 'rgb(246, 127, 255)'
				}
			]
		},
		options: {
			title: {
				display: true,
				text: nodeNames[i] + " Usage",
				fontColor: 'rgb(72, 244, 66)'
			},
			responsive: true,
			scales: {
				yAxes: [
					{
						id: 'cpu',
						ticks: {
							beginAtZero: true,
							min: 0,
							max: 100,
							maxTicksLimit: 10,
							stepSize: 10,
							display: true
						},
						scaleLabel: {
							display: true,
							labelString: 'Percent Usage',
							fontColor: 'rgb(72, 244, 66)'
						}
					}
				],
				xAxes: [
					{
						type: 'realtime',
						ticks: {
							maxTicksLimit: 3,
						},
						scaleLabel: {
							display: true,
							labelString: 'Time',
							fontColor: 'rgb(72, 244, 66)'
						},
						realtime: {
							onRefresh: function(chart) {
								chart.data.datasets[0].data.push({
									x: Date.now(),
									y: cpuData
								});
								chart.data.datasets[1].data.push({
									x: Date.now(),
									y: ramData
								});
							},
							delay: 1000,
							duration: 10000
						}
					}
				]
			},
			plugins: {
				streaming: {
					frameRate: 30
				}
			}
		}
	});
	
	let newSocket = new WebSocket(nodeUsageAddresses[i]);
	newSocket.onmessage = function(event) {
		let loads = event.data.split(' ');
		cpuData = Number(loads[0]);
		ramData = Number(loads[1]);
	}
	sockets.push(newSocket);
	charts.push(chart);
}